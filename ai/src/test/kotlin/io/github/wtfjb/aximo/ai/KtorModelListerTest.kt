package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class KtorModelListerTest {

    private suspend fun expectError(block: suspend () -> Unit): AiException {
        try {
            block()
        } catch (e: AiException) {
            return e
        }
        fail("Expected AiException")
        throw AssertionError()
    }

    @Test
    fun openAiCompatibleListsSortedIds() = runBlocking {
        val server = MockServer {
            json("""{"object":"list","data":[{"id":"mistral-small-latest"},{"id":"Codestral-latest"},{"id":"mistral-large-latest"},{"id":"mistral-small-latest"},{"name":"no id"}]}""")
        }

        val models = KtorModelLister(server.client).listModels(AiProviderKind.OPENAI_COMPATIBLE, "https://api.mistral.ai/v1/", "sk-secret")

        assertEquals(listOf("Codestral-latest", "mistral-large-latest", "mistral-small-latest"), models)
        val request = server.lastRequest
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("https://api.mistral.ai/v1/models", request.url.toString())
        assertEquals("Bearer sk-secret", request.headers["Authorization"])
    }

    @Test
    fun withoutKeyNoAuthorizationHeader() = runBlocking {
        val server = MockServer { json("""{"data":[{"id":"llama3.2:latest"}]}""") }

        val models = KtorModelLister(server.client).listModels(AiProviderKind.OPENAI_COMPATIBLE, "http://192.168.1.10:11434/v1", null)

        assertEquals(listOf("llama3.2:latest"), models)
        assertNull(server.lastRequest.headers["Authorization"])
    }

    @Test
    fun anthropicUsesItsHeadersAndPath() = runBlocking {
        val server = MockServer {
            json("""{"data":[{"type":"model","id":"claude-sonnet-5-5","display_name":"Claude Sonnet 5.5"},{"type":"model","id":"claude-haiku-4-5"}],"has_more":false}""")
        }
        val lister = KtorModelLister(server.client)

        val models = lister.listModels(AiProviderKind.ANTHROPIC, "https://api.anthropic.com", "sk-ant")

        assertEquals(listOf("claude-haiku-4-5", "claude-sonnet-5-5"), models)
        val request = server.lastRequest
        assertEquals("https://api.anthropic.com/v1/models?limit=1000", request.url.toString())
        assertEquals("sk-ant", request.headers["x-api-key"])
        assertEquals(AnthropicProvider.API_VERSION, request.headers["anthropic-version"])

        lister.listModels(AiProviderKind.ANTHROPIC, "https://api.anthropic.com/v1", "sk-ant")
        assertEquals("https://api.anthropic.com/v1/models?limit=1000", server.lastRequest.url.toString())
    }

    @Test
    fun errorsAreMapped() = runBlocking {
        val unauthorized = MockServer { json("""{"error":{"message":"bad key sk-secret"}}""", HttpStatusCode.Unauthorized) }
        val e = expectError { KtorModelLister(unauthorized.client).listModels(AiProviderKind.OPENAI_COMPATIBLE, "https://x.example/v1", "sk-secret") }
        assertEquals(AiException.Reason.UNAUTHORIZED, e.reason)
        assertEquals("bad key ***", e.detail)

        val notFound = MockServer { json("""{"detail":"Not Found"}""", HttpStatusCode.NotFound) }
        assertEquals(
            AiException.Reason.NOT_FOUND,
            expectError { KtorModelLister(notFound.client).listModels(AiProviderKind.OPENAI_COMPATIBLE, "https://x.example", null) }.reason,
        )

        val wrongShape = MockServer { json("""{"models":[]}""") }
        assertEquals(
            AiException.Reason.INVALID_RESPONSE,
            expectError { KtorModelLister(wrongShape.client).listModels(AiProviderKind.OPENAI_COMPATIBLE, "https://x.example/v1", null) }.reason,
        )
    }
}
