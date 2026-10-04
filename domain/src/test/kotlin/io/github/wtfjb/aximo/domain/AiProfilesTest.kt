package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.ai.AiConnectionResult
import io.github.wtfjb.aximo.domain.ai.AiConnectionTester
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiModels
import io.github.wtfjb.aximo.domain.ai.AiProfileDraft
import io.github.wtfjb.aximo.domain.ai.AiProfileFieldError
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProfilesTest {

    private fun profile(id: Long, active: Boolean = false) =
        AiProviderProfile(id = id, name = "P$id", kind = AiProviderKind.OPENAI_COMPATIBLE, baseUrl = "http://nas:11434/v1", model = "llama", active = active)

    @Test
    fun validatesBaseUrls() {
        listOf("https://api.anthropic.com", "http://192.168.1.10:11434/v1", "https://openrouter.ai/api/v1/", "HTTPS://Example.com").forEach {
            assertTrue(it, AiProfiles.isValidBaseUrl(it))
        }
        listOf("", "api.openai.com", "ftp://x", "https://", "https://a b", "https://x/v1?key=1").forEach {
            assertFalse(it, AiProfiles.isValidBaseUrl(it))
        }
    }

    @Test
    fun normalizesAndDetectsCleartext() {
        assertEquals("https://x/v1", AiProfiles.normalizeBaseUrl("  https://x/v1// "))
        assertTrue(AiProfiles.isCleartext("http://nas:11434/v1"))
        assertFalse(AiProfiles.isCleartext("https://api.mistral.ai/v1"))
    }

    @Test
    fun activeIsFlaggedOrFirst() {
        assertNull(AiProfiles.active(emptyList()))
        assertEquals(2L, AiProfiles.active(listOf(profile(1), profile(2, active = true)))?.id)
        assertEquals(1L, AiProfiles.active(listOf(profile(1), profile(2)))?.id)
        assertFalse(AiProfiles.isAvailable(emptyList()))
        assertTrue(AiProfiles.isAvailable(listOf(profile(1))))
    }

    @Test
    fun draftNeedsNameUrlModel() {
        assertEquals(
            setOf(AiProfileFieldError.NAME_MISSING, AiProfileFieldError.URL_INVALID, AiProfileFieldError.MODEL_MISSING),
            AiProfileDraft().errors(),
        )
        val ollama = AiProfileDraft(name = " Ollama ", baseUrl = "http://nas:11434/v1/", model = " llama3.2 ")
        assertTrue(ollama.errors().isEmpty())
        val saved = ollama.toProfile()!!
        assertEquals("Ollama", saved.name)
        assertEquals("http://nas:11434/v1", saved.baseUrl)
        assertEquals("llama3.2", saved.model)
        assertFalse(saved.hasApiKey)
        assertTrue(ollama.isCleartext)
    }

    @Test
    fun anthropicNeedsAKey() {
        val draft = AiProfileDraft(name = "Claude", model = "claude-x").withKind(AiProviderKind.ANTHROPIC)
        assertEquals(AiProfiles.ANTHROPIC_BASE_URL, draft.baseUrl)
        assertEquals(setOf(AiProfileFieldError.KEY_MISSING), draft.errors())
        assertNull(draft.toProfile())

        assertTrue(draft.copy(apiKey = "sk").errors().isEmpty())
        assertTrue(draft.copy(hasStoredKey = true).errors().isEmpty())
        assertEquals(setOf(AiProfileFieldError.KEY_MISSING), draft.copy(hasStoredKey = true, removeKey = true).errors())
    }

    @Test
    fun switchingKindOnlyTouchesDefaultUrl() {
        val anthropic = AiProfileDraft().withKind(AiProviderKind.ANTHROPIC)
        assertEquals("", anthropic.withKind(AiProviderKind.OPENAI_COMPATIBLE).baseUrl)

        val own = AiProfileDraft(baseUrl = "https://proxy.example.com")
        assertEquals("https://proxy.example.com", own.withKind(AiProviderKind.ANTHROPIC).baseUrl)
        assertEquals("https://proxy.example.com", own.withKind(AiProviderKind.ANTHROPIC).withKind(AiProviderKind.OPENAI_COMPATIBLE).baseUrl)
    }

    @Test
    fun keyChange() {
        val stored = AiProfileDraft(hasStoredKey = true)
        assertEquals(ApiKeyChange.Keep, stored.keyChange())
        assertEquals(ApiKeyChange.Remove, stored.copy(removeKey = true).keyChange())
        val set = stored.copy(apiKey = " sk-new ").keyChange()
        assertTrue(set is ApiKeyChange.Set)
        assertEquals("sk-new", (set as ApiKeyChange.Set).value)
    }

    @Test
    fun keyNeverInToString() {
        assertFalse(AiProfileDraft(apiKey = "sk-secret").toString().contains("sk-secret"))
        assertFalse(ApiKeyChange.Set("sk-secret").toString().contains("sk-secret"))
    }

    @Test
    fun fromProfileKeepsIdAndKeyFlag() {
        val draft = AiProfileDraft.from(profile(5, active = true).copy(hasApiKey = true))
        assertEquals(5L, draft.id)
        assertTrue(draft.hasStoredKey)
        assertTrue(draft.active)
        assertEquals("", draft.apiKey)
        assertFalse(draft.isNew)
    }

    @Test
    fun connectionTesterPassesBlankKeyAsNull() = runTest {
        var usedKey: String? = "unset"
        var sent: AiRequest? = null
        val tester = AiConnectionTester { _, key ->
            usedKey = key
            object : AiProvider {
                override suspend fun complete(request: AiRequest): String {
                    sent = request
                    return "OK"
                }
            }
        }

        assertEquals(AiConnectionResult.Success, tester.test(profile(1), " "))
        assertNull(usedKey)
        assertEquals(AiMessage.Role.USER, sent!!.messages.single().role)
    }

    @Test
    fun connectionTesterReportsError() = runTest {
        val tester = AiConnectionTester { _, _ ->
            object : AiProvider {
                override suspend fun complete(request: AiRequest): String = throw AiException(AiException.Reason.TIMEOUT)
            }
        }

        val result = tester.test(profile(1), "k")

        assertEquals(AiException.Reason.TIMEOUT, (result as AiConnectionResult.Failure).error.reason)
    }

    @Test
    fun canListModelsNeedsUrlAndKey() {
        assertFalse(AiProfileDraft().canListModels)
        val openAi = AiProfileDraft(baseUrl = "https://api.mistral.ai/v1")
        assertFalse(openAi.canListModels)
        assertFalse(AiProfileDraft(apiKey = "sk").canListModels)
        assertTrue(openAi.copy(apiKey = "sk").canListModels)
        val anthropic = AiProfileDraft().withKind(AiProviderKind.ANTHROPIC)
        assertFalse(anthropic.canListModels)
        assertTrue(anthropic.copy(apiKey = "sk").canListModels)
        assertTrue(anthropic.copy(hasStoredKey = true).canListModels)
        assertFalse(anthropic.copy(hasStoredKey = true, removeKey = true).canListModels)
    }

    @Test
    fun modelIdsAreCleaned() {
        assertEquals(listOf("a-model", "B-model", "c"), AiModels.clean(listOf("c", " B-model ", "", "a-model", "c")))
    }
}
