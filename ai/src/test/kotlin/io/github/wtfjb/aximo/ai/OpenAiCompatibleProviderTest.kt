package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class OpenAiCompatibleProviderTest {

    private val ok = """{"id":"x","choices":[{"index":0,"message":{"role":"assistant","content":"Hallo"},"finish_reason":"stop"}]}"""

    private val request = AiRequest(
        system = "You are a coach.",
        messages = listOf(AiMessage(AiMessage.Role.USER, "Hi"), AiMessage(AiMessage.Role.ASSISTANT, "Hey"), AiMessage(AiMessage.Role.USER, "Plan?")),
    )

    private fun provider(server: MockServer, key: String? = "sk-secret", baseUrl: String = "https://api.example.com/v1") =
        OpenAiCompatibleProvider(server.client, baseUrl, "gpt-test", key)

    @Test
    fun sendsChatCompletionsRequest() = runBlocking {
        val server = MockServer { json(ok) }

        val answer = provider(server).complete(request.copy(maxTokens = 500))

        assertEquals("Hallo", answer)
        val sent = server.lastRequest
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("https://api.example.com/v1/chat/completions", sent.url.toString())
        assertEquals("Bearer sk-secret", sent.headers["Authorization"])
        val body = sent.jsonBody()
        assertEquals("gpt-test", body["model"]!!.jsonPrimitive.content)
        assertEquals(500, body["max_tokens"]!!.jsonPrimitive.content.toInt())
        assertEquals(JsonPrimitive(false), body["stream"])
        val messages = body["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("system", "user", "assistant", "user"), messages.map { it["role"]!!.jsonPrimitive.content })
        assertEquals("You are a coach.", messages[0]["content"]!!.jsonPrimitive.content)
        assertEquals("Plan?", messages[3]["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun withoutKeyAndLimitNoAuthorizationAndNoMaxTokens() = runBlocking {
        val server = MockServer { json(ok) }

        provider(server, key = null, baseUrl = "http://192.168.1.10:11434/v1").complete(AiRequest(listOf(AiMessage(AiMessage.Role.USER, "Hi"))))

        assertNull(server.lastRequest.headers["Authorization"])
        assertFalse(server.lastRequest.jsonBody().containsKey("max_tokens"))
        assertFalse(server.lastRequest.jsonBody()["messages"]!!.jsonArray.any { it.jsonObject["role"]!!.jsonPrimitive.content == "system" })
        assertEquals("http://192.168.1.10:11434/v1/chat/completions", server.lastRequest.url.toString())
    }

    @Test
    fun nullContentIsAnEmptyAnswer() = runBlocking {
        val server = MockServer { json("""{"choices":[{"message":{"role":"assistant","content":null}}]}""") }

        assertEquals("", provider(server).complete(request))
    }

    @Test
    fun mapsStatusCodes() = runBlocking {
        val cases = mapOf(
            401 to AiException.Reason.UNAUTHORIZED,
            403 to AiException.Reason.UNAUTHORIZED,
            404 to AiException.Reason.NOT_FOUND,
            400 to AiException.Reason.REJECTED,
            422 to AiException.Reason.REJECTED,
            429 to AiException.Reason.RATE_LIMITED,
            500 to AiException.Reason.SERVER,
            503 to AiException.Reason.SERVER,
        )
        cases.forEach { (status, reason) ->
            val server = MockServer { json("""{"error":{"message":"nope"}}""", HttpStatusCode.fromValue(status)) }
            val error = expectError { provider(server).complete(request) }
            assertEquals("HTTP $status", reason, error.reason)
            assertEquals(status, error.statusCode)
            assertEquals("nope", error.detail)
        }
    }

    @Test
    fun errorDetailNeverContainsTheKey() = runBlocking {
        val server = MockServer { json("""{"error":{"message":"Incorrect API key provided: sk-secret"}}""", HttpStatusCode.Unauthorized) }

        val error = expectError { provider(server).complete(request) }

        assertEquals("Incorrect API key provided: ***", error.detail)
        assertFalse(error.toString().contains("sk-secret"))
        assertFalse(error.message!!.contains("sk-secret"))
    }

    @Test
    fun readsOtherErrorShapes() = runBlocking {
        val ollama = MockServer { json("""{"error":"model \"llama9\" not found"}""", HttpStatusCode.NotFound) }
        assertEquals("model \"llama9\" not found", expectError { provider(ollama).complete(request) }.detail)

        val mistral = MockServer { json("""{"message":"Invalid model"}""", HttpStatusCode.BadRequest) }
        assertEquals("Invalid model", expectError { provider(mistral).complete(request) }.detail)

        val html = MockServer { respond("<html>Bad Gateway</html>", HttpStatusCode.BadGateway) }
        val error = expectError { provider(html).complete(request) }
        assertEquals(AiException.Reason.SERVER, error.reason)
        assertNull(error.detail)
    }

    @Test
    fun errorWithStatus200IsRejected() = runBlocking {
        val server = MockServer { json("""{"error":{"message":"No credits","code":402}}""") }

        val error = expectError { provider(server).complete(request) }

        assertEquals(AiException.Reason.REJECTED, error.reason)
        assertEquals("No credits", error.detail)
    }

    @Test
    fun brokenResponseIsInvalid() = runBlocking {
        listOf("not json", """{"choices":[]}""", """{"object":"list"}""").forEach { body ->
            val server = MockServer { json(body) }
            assertEquals(body, AiException.Reason.INVALID_RESPONSE, expectError { provider(server).complete(request) }.reason)
        }
    }

    @Test
    fun contentFilterIsRefused() = runBlocking {
        val server = MockServer { json("""{"choices":[{"message":{"role":"assistant","content":""},"finish_reason":"content_filter"}]}""") }

        assertEquals(AiException.Reason.REFUSED, expectError { provider(server).complete(request) }.reason)
    }

    @Test
    fun slowServerTimesOut() = runBlocking {
        val server = MockServer(AiTimeouts(connectMillis = 100, socketMillis = 100, requestMillis = 100)) {
            delay(5_000)
            json(ok)
        }

        assertEquals(AiException.Reason.TIMEOUT, expectError { provider(server).complete(request) }.reason)
    }

    @Test
    fun connectionProblemIsNetwork() = runBlocking {
        val server = MockServer { throw IOException("Connection refused") }

        val error = expectError { provider(server).complete(request) }

        assertEquals(AiException.Reason.NETWORK, error.reason)
        assertTrue(error.cause is IOException)
    }
}

/** Runs [block] and returns the AiException it throws. */
suspend fun expectError(block: suspend () -> Unit): AiException {
    try {
        block()
    } catch (e: AiException) {
        return e
    }
    fail("Expected an AiException")
    throw AssertionError()
}
