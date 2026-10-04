package io.github.wtfjb.aximo.ui.ai

import io.github.wtfjb.aximo.domain.ai.AiConnectionTester
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProfileFieldError
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiProfileViewModelsTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repo = FakeAiProfileRepository()

    /** Records what the connection test sent and answers with [answer]. */
    private var usedKey: String? = null
    private var answer: suspend () -> String = { "OK" }
    private val tester = AiConnectionTester { _, key ->
        usedKey = key
        object : AiProvider {
            override suspend fun complete(request: AiRequest): String = answer()
        }
    }

    private fun editVm(id: Long = 0) = AiProfileEditViewModel(repo, tester, id)

    @Test
    fun createsProfileWithKey() = runTest(UnconfinedTestDispatcher()) {
        val vm = editVm()
        vm.setName("Claude")
        vm.setKind(AiProviderKind.ANTHROPIC)
        vm.setModel("claude-sonnet-5-5")

        vm.save()
        assertEquals(setOf(AiProfileFieldError.KEY_MISSING), vm.uiState.value.errors)
        assertFalse(vm.uiState.value.done)

        vm.setApiKey("sk-ant")
        vm.save()

        assertTrue(vm.uiState.value.done)
        val saved = repo.observeProfiles().first().single()
        assertEquals("https://api.anthropic.com", saved.baseUrl)
        assertTrue(saved.active)
        assertEquals("sk-ant", repo.apiKey(saved.id))
    }

    @Test
    fun errorsOnlyAfterFirstAttempt() = runTest(UnconfinedTestDispatcher()) {
        val vm = editVm()
        assertTrue(vm.uiState.value.errors.isEmpty())

        vm.save()

        assertEquals(
            setOf(AiProfileFieldError.NAME_MISSING, AiProfileFieldError.URL_INVALID, AiProfileFieldError.MODEL_MISSING),
            vm.uiState.value.errors,
        )
    }

    @Test
    fun editingKeepsStoredKeyUnlessReplacedOrRemoved() = runTest(UnconfinedTestDispatcher()) {
        val id = repo.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Set("old"))

        val vm = editVm(id)
        assertTrue(vm.uiState.value.draft.hasStoredKey)
        assertEquals("", vm.uiState.value.draft.apiKey)
        vm.setModel("qwen3")
        vm.save()
        assertEquals("old", repo.apiKey(id))
        assertEquals("qwen3", repo.getProfile(id)!!.model)

        val vm2 = editVm(id)
        vm2.toggleRemoveKey()
        vm2.save()
        assertNull(repo.apiKey(id))
    }

    @Test
    fun connectionTestUsesTypedOrStoredKey() = runTest(UnconfinedTestDispatcher()) {
        val id = repo.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Set("stored"))
        val vm = editVm(id)

        vm.testConnection()
        assertEquals(ConnectionTestState.Success, vm.uiState.value.test)
        assertEquals("stored", usedKey)

        vm.setApiKey("typed")
        assertEquals(ConnectionTestState.Idle, vm.uiState.value.test)
        vm.testConnection()
        assertEquals("typed", usedKey)

        vm.setApiKey("")
        vm.toggleRemoveKey()
        vm.testConnection()
        assertNull(usedKey)
        // Testing does not save anything.
        assertEquals("stored", repo.apiKey(id))
    }

    @Test
    fun connectionTestShowsRunningAndFailure() = runTest(UnconfinedTestDispatcher()) {
        val gate = CompletableDeferred<String>()
        answer = { gate.await() }
        val vm = editVm()
        vm.setName("Ollama")
        vm.setBaseUrl("http://nas:11434/v1")
        vm.setModel("llama3.2")

        vm.testConnection()
        assertEquals(ConnectionTestState.Running, vm.uiState.value.test)

        gate.completeExceptionally(AiException(AiException.Reason.NOT_FOUND, 404, "model not found"))
        assertEquals(ConnectionTestState.Failed(AiException.Reason.NOT_FOUND, 404, "model not found"), vm.uiState.value.test)
        assertTrue(repo.observeProfiles().first().isEmpty())
    }

    @Test
    fun connectionTestWithInvalidFormShowsErrors() = runTest(UnconfinedTestDispatcher()) {
        val vm = editVm()

        vm.testConnection()

        assertEquals(ConnectionTestState.Idle, vm.uiState.value.test)
        assertTrue(vm.uiState.value.errors.isNotEmpty())
    }

    @Test
    fun deleteRemovesProfileAndKey() = runTest(UnconfinedTestDispatcher()) {
        val id = repo.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Set("k"))
        val vm = editVm(id)

        vm.delete()

        assertTrue(vm.uiState.value.done)
        assertNull(repo.getProfile(id))
        assertNull(repo.apiKey(id))
    }

    @Test
    fun listShowsProfilesAndSwitchesActive() = runTest(UnconfinedTestDispatcher()) {
        val vm = AiProfilesViewModel(repo)
        vm.uiState.launchIn(backgroundScope)
        assertTrue(vm.uiState.value.profiles.isEmpty())
        assertFalse(vm.uiState.value.loading)

        val first = repo.saveProfile(FakeAiProfileRepository.profile("A"), ApiKeyChange.Keep)
        val second = repo.saveProfile(FakeAiProfileRepository.profile("B"), ApiKeyChange.Keep)
        assertEquals(first, vm.uiState.value.activeId)

        vm.setActive(second)
        assertEquals(second, vm.uiState.value.activeId)
        assertEquals(listOf("A", "B"), vm.uiState.value.profiles.map { it.name })
    }
}
