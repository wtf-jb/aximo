package io.github.wtfjb.aximo.ui.ai

import io.github.wtfjb.aximo.domain.ai.AiConnectionTester
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiModelLister
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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

    /** Records model list requests and answers with [models]. */
    private val modelRequests = mutableListOf<Triple<AiProviderKind, String, String?>>()
    private var models: suspend () -> List<String> = { listOf("mistral-large-latest", "mistral-small-latest") }
    private val lister = AiModelLister { kind, url, key ->
        modelRequests += Triple(kind, url, key)
        models()
    }

    private fun editVm(id: Long = 0, debounce: Long = 0) = AiProfileEditViewModel(repo, tester, lister, id, debounce)

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

    @Test
    fun modelsLoadOnlyWithUrlAndKey() = runTest(UnconfinedTestDispatcher()) {
        val vm = editVm()
        vm.setModel("x")
        assertEquals(ModelListState.Idle, vm.uiState.value.models)

        vm.setBaseUrl("https://api.mistral.ai/v1")
        assertEquals(ModelListState.Idle, vm.uiState.value.models)
        assertTrue(modelRequests.isEmpty())

        vm.setApiKey("sk")

        assertEquals(ModelListState.Loaded(listOf("mistral-large-latest", "mistral-small-latest")), vm.uiState.value.models)
        assertEquals(Triple(AiProviderKind.OPENAI_COMPATIBLE, "https://api.mistral.ai/v1", "sk"), modelRequests.single())

        vm.setBaseUrl("not a url")
        assertEquals(ModelListState.Idle, vm.uiState.value.models)

        vm.setBaseUrl("https://api.mistral.ai/v1")
        vm.setApiKey("")
        assertEquals(ModelListState.Idle, vm.uiState.value.models)
    }

    @Test
    fun anthropicLoadsOnceTheKeyIsThere() = runTest(UnconfinedTestDispatcher()) {
        val vm = editVm()
        vm.setKind(AiProviderKind.ANTHROPIC)
        assertEquals(ModelListState.Idle, vm.uiState.value.models)
        assertTrue(modelRequests.isEmpty())

        vm.setApiKey("sk-ant")

        assertEquals(Triple(AiProviderKind.ANTHROPIC, "https://api.anthropic.com", "sk-ant"), modelRequests.single())
    }

    @Test
    fun existingProfileLoadsWithStoredKey() = runTest(UnconfinedTestDispatcher()) {
        val id = repo.saveProfile(FakeAiProfileRepository.profile("Mistral"), ApiKeyChange.Set("stored"))

        val vm = editVm(id)

        assertEquals("stored", modelRequests.single().third)
        assertTrue(vm.uiState.value.models is ModelListState.Loaded)
    }

    @Test
    fun pickingAModelOnlySetsTheField() = runTest(UnconfinedTestDispatcher()) {
        val vm = editVm()
        vm.setBaseUrl("https://api.mistral.ai/v1")
        vm.setApiKey("sk")

        vm.setModel("mistral-small-latest")

        assertEquals("mistral-small-latest", vm.uiState.value.draft.model)
        assertEquals(1, modelRequests.size) // choosing a model does not reload the list
    }

    @Test
    fun failureCanBeRetried() = runTest(UnconfinedTestDispatcher()) {
        models = { throw AiException(AiException.Reason.UNAUTHORIZED, 401, "bad key") }
        val vm = editVm()
        vm.setBaseUrl("https://api.mistral.ai/v1")
        vm.setApiKey("bad")
        assertEquals(ModelListState.Failed(AiException.Reason.UNAUTHORIZED, 401, "bad key"), vm.uiState.value.models)

        models = { listOf("a") }
        vm.reloadModels()

        assertEquals(ModelListState.Loaded(listOf("a")), vm.uiState.value.models)
    }

    @Test
    fun typingIsDebounced() = runTest {
        val vm = editVm(debounce = AiProfileEditViewModel.MODEL_DEBOUNCE_MILLIS)

        vm.setApiKey("sk")
        vm.setBaseUrl("https://api.mistral.ai/v")
        advanceTimeBy(AiProfileEditViewModel.MODEL_DEBOUNCE_MILLIS / 2)
        vm.setBaseUrl("https://api.mistral.ai/v1")
        advanceTimeBy(AiProfileEditViewModel.MODEL_DEBOUNCE_MILLIS / 2)
        assertTrue(modelRequests.isEmpty())

        advanceUntilIdle()

        assertEquals("https://api.mistral.ai/v1", modelRequests.single().second)
    }
}
