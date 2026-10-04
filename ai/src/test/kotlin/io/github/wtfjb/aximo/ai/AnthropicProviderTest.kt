package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class AnthropicProviderTest {

    private val ok = """{"id":"msg_1","type":"message","role":"assistant","content":[
        {"type":"thinking","thinking":""},{"type":"text","text":"Hal"},{"type":"text","text":"lo"}],
        "stop_reason":"end_turn","usage":{"input_tokens":3,"output_tokens":2}}"""

    private val request = AiRequest(
        system = "You are a coach.",
        messages = listOf(AiMessage(AiMessage.Role.USER, "Hi"), AiMessage(AiMessage.Role.ASSISTANT, "Hey"), AiMessage(AiMessage.Role.USER, "Plan?")),
    )

    private fun provider(server: MockServer, key: String? = "sk-ant-secret", baseUrl: String = "https://api.anthropic.com") =
        AnthropicProvider(server.client, baseUrl, "claude-test", key)

    @Test
    fun sendsMessagesRequest() = runBlocking {
        val server = MockServer { json(ok) }

        val answer = provider(server).complete(request.copy(maxTokens = 800))

        assertEquals("Hallo", answer)
        val sent = server.lastRequest
        assertEquals("https://api.anthropic.com/v1/messages", sent.url.toString())
        assertEquals("sk-ant-secret", sent.headers["x-api-key"])
        assertEquals("2023-06-01", sent.headers["anthropic-version"])
        assertNull(sent.headers["Authorization"])
        val body = sent.jsonBody()
        assertEquals("claude-test", body["model"]!!.jsonPrimitive.content)
        assertEquals(800, body["max_tokens"]!!.jsonPrimitive.content.toInt())
        assertEquals("You are a coach.", body["system"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("user", "assistant", "user"), messages.map { it["role"]!!.jsonPrimitive.content })
    }

    @Test
    fun defaultMaxTokensAndNoSystem() = runBlocking {
        val server = MockServer { json(ok) }

        provider(server).complete(AiRequest(listOf(AiMessage(AiMessage.Role.USER, "Hi"))))

        val body = server.lastRequest.jsonBody()
        assertEquals(AnthropicProvider.DEFAULT_MAX_TOKENS, body["max_tokens"]!!.jsonPrimitive.content.toInt())
        assertFalse(body.containsKey("system"))
    }

    @Test
    fun acceptsBaseUrlWithVersion() = runBlocking {
        val server = MockServer { json(ok) }

        provider(server, baseUrl = "https://proxy.example.com/v1").complete(request)

        assertEquals("https://proxy.example.com/v1/messages", server.lastRequest.url.toString())
    }

    @Test
    fun mapsErrors() = runBlocking {
        val cases = mapOf(
            401 to AiException.Reason.UNAUTHORIZED,
            404 to AiException.Reason.NOT_FOUND,
            400 to AiException.Reason.REJECTED,
            429 to AiException.Reason.RATE_LIMITED,
            529 to AiException.Reason.SERVER,
        )
        cases.forEach { (status, reason) ->
            val server = MockServer {
                json("""{"type":"error","error":{"type":"x","message":"bad sk-ant-secret"}}""", HttpStatusCode.fromValue(status))
            }
            val error = expectError { provider(server).complete(request) }
            assertEquals("HTTP $status", reason, error.reason)
            assertEquals("bad ***", error.detail)
        }
    }

    @Test
    fun refusalIsReported() = runBlocking {
        val server = MockServer { json("""{"content":[],"stop_reason":"refusal"}""") }

        assertEquals(AiException.Reason.REFUSED, expectError { provider(server).complete(request) }.reason)
    }

    @Test
    fun missingContentIsInvalid() = runBlocking {
        val server = MockServer { json("""{"id":"msg_1"}""") }

        assertEquals(AiException.Reason.INVALID_RESPONSE, expectError { provider(server).complete(request) }.reason)
    }

    @Test
    fun timeoutAndNetwork() = runBlocking {
        val slow = MockServer(AiTimeouts(connectMillis = 100, socketMillis = 100, requestMillis = 100)) {
            delay(5_000)
            json(ok)
        }
        assertEquals(AiException.Reason.TIMEOUT, expectError { provider(slow).complete(request) }.reason)

        val offline = MockServer { throw IOException("Unable to resolve host") }
        assertEquals(AiException.Reason.NETWORK, expectError { provider(offline).complete(request) }.reason)
    }
}
