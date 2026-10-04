package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiConnectionResult
import io.github.wtfjb.aximo.domain.ai.AiConnectionTester
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorAiProviderFactoryTest {

    @Test
    fun picksAdapterByKindAndTrimsSlash() = runBlocking {
        val server = MockServer { request ->
            if (request.url.encodedPath.endsWith("/v1/messages")) {
                json("""{"content":[{"type":"text","text":"OK"}],"stop_reason":"end_turn"}""")
            } else {
                json("""{"choices":[{"message":{"role":"assistant","content":"OK"}}]}""")
            }
        }
        val factory = KtorAiProviderFactory(server.client)
        val tester = AiConnectionTester(factory)

        val openAi = AiProviderProfile(name = "Ollama", kind = AiProviderKind.OPENAI_COMPATIBLE, baseUrl = "http://nas:11434/v1/", model = "llama3.2")
        assertEquals(AiConnectionResult.Success, tester.test(openAi, null))
        assertEquals("http://nas:11434/v1/chat/completions", server.lastRequest.url.toString())

        val anthropic = AiProviderProfile(name = "Claude", kind = AiProviderKind.ANTHROPIC, baseUrl = "https://api.anthropic.com/", model = "claude-test")
        assertEquals(AiConnectionResult.Success, tester.test(anthropic, "key"))
        assertEquals("https://api.anthropic.com/v1/messages", server.lastRequest.url.toString())
    }

    @Test
    fun connectionTestReportsFailure() = runBlocking {
        val server = MockServer { json("""{"error":{"message":"invalid x-api-key"}}""", HttpStatusCode.Unauthorized) }
        val tester = AiConnectionTester(KtorAiProviderFactory(server.client))
        val profile = AiProviderProfile(name = "Claude", kind = AiProviderKind.ANTHROPIC, baseUrl = "https://api.anthropic.com", model = "claude-test")

        val result = tester.test(profile, "wrong")

        assertTrue(result is AiConnectionResult.Failure)
        assertEquals(AiException.Reason.UNAUTHORIZED, (result as AiConnectionResult.Failure).error.reason)
    }
}
