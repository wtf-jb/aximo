package io.github.wtfjb.aximo.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiConnectionResult
import io.github.wtfjb.aximo.domain.ai.AiConnectionTester
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiModelLister
import io.github.wtfjb.aximo.domain.ai.AiProfileDraft
import io.github.wtfjb.aximo.domain.ai.AiProfileFieldError
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of "Verbindung testen". */
sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Running : ConnectionTestState
    data object Success : ConnectionTestState
    data class Failed(val reason: AiException.Reason, val statusCode: Int?, val detail: String?) : ConnectionTestState
}

/** The model list for the dropdown, loaded from the provider. */
sealed interface ModelListState {
    /** Not enough input yet (URL, for Anthropic also a key). */
    data object Idle : ModelListState
    data object Loading : ModelListState
    data class Loaded(val models: List<String>) : ModelListState
    data class Failed(val reason: AiException.Reason, val statusCode: Int?, val detail: String?) : ModelListState
}

data class AiProfileEditUiState(
    val draft: AiProfileDraft = AiProfileDraft(),
    val loading: Boolean = false,
    /** Errors appear after the first save or test attempt. */
    val showErrors: Boolean = false,
    val test: ConnectionTestState = ConnectionTestState.Idle,
    val models: ModelListState = ModelListState.Idle,
    /** Saved or deleted: the screen closes. */
    val done: Boolean = false,
) {
    val errors: Set<AiProfileFieldError> get() = if (showErrors) draft.errors() else emptySet()
}

/** Create (profileId = 0) or edit a provider profile (B-01). */
class AiProfileEditViewModel(
    private val repository: AiProfileRepository,
    private val tester: AiConnectionTester,
    private val lister: AiModelLister,
    profileId: Long,
    /** Wait after typing before asking the provider, so not every key press sends a request. */
    private val modelDebounceMillis: Long = MODEL_DEBOUNCE_MILLIS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiProfileEditUiState(loading = profileId != 0L))
    val uiState: StateFlow<AiProfileEditUiState> = _uiState.asStateFlow()

    private var testJob: Job? = null
    private var modelJob: Job? = null

    init {
        if (profileId != 0L) {
            viewModelScope.launch {
                val stored = repository.getProfile(profileId)
                _uiState.update { state ->
                    state.copy(draft = stored?.let { AiProfileDraft.from(it) } ?: state.draft, loading = false)
                }
                scheduleModelLoad(delayMillis = 0)
            }
        }
    }

    fun setName(value: String) = edit { it.copy(name = value) }
    fun setKind(kind: AiProviderKind) = editConnection { it.withKind(kind) }
    fun setBaseUrl(value: String) = editConnection { it.copy(baseUrl = value) }
    fun setModel(value: String) = edit { it.copy(model = value) }
    fun setApiKey(value: String) = editConnection { it.copy(apiKey = value, removeKey = false) }

    /** Marks the stored key for removal on save (or undoes that). */
    fun toggleRemoveKey() = editConnection { it.copy(removeKey = !it.removeKey, apiKey = "") }

    /** "Erneut laden" after an error. */
    fun reloadModels() = scheduleModelLoad(delayMillis = 0)

    /**
     * Loads the model list once URL (and for Anthropic a key) are there. Each
     * change to them starts over; the answer of an older request is dropped.
     */
    private fun scheduleModelLoad(delayMillis: Long = modelDebounceMillis) {
        modelJob?.cancel()
        val draft = _uiState.value.draft
        if (!draft.canListModels) {
            _uiState.update { it.copy(models = ModelListState.Idle) }
            return
        }
        modelJob = viewModelScope.launch {
            delay(delayMillis)
            _uiState.update { it.copy(models = ModelListState.Loading) }
            val result = try {
                ModelListState.Loaded(lister.listModels(draft.kind, draft.baseUrl, currentKey(draft)))
            } catch (e: AiException) {
                ModelListState.Failed(e.reason, e.statusCode, e.detail)
            }
            _uiState.update { it.copy(models = result) }
        }
    }

    /** The key typed now, else the stored one (unless it is marked for removal). */
    private suspend fun currentKey(draft: AiProfileDraft): String? = when {
        draft.apiKey.isNotBlank() -> draft.apiKey.trim()
        draft.hasStoredKey && !draft.removeKey -> repository.apiKey(draft.id)
        else -> null
    }

    /** Sends a tiny request with the values in the form, without saving them. */
    fun testConnection() {
        val draft = _uiState.value.draft
        val profile = draft.toProfile()
        if (profile == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        testJob?.cancel()
        _uiState.update { it.copy(test = ConnectionTestState.Running) }
        testJob = viewModelScope.launch {
            val result = when (val outcome = tester.test(profile, currentKey(draft))) {
                AiConnectionResult.Success -> ConnectionTestState.Success
                is AiConnectionResult.Failure -> outcome.error.let { ConnectionTestState.Failed(it.reason, it.statusCode, it.detail) }
            }
            _uiState.update { it.copy(test = result) }
        }
    }

    fun save() {
        val draft = _uiState.value.draft
        val profile = draft.toProfile()
        if (profile == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            repository.saveProfile(profile, draft.keyChange())
            _uiState.update { it.copy(done = true) }
        }
    }

    /** Deletes the profile and its key; the screen asks for confirmation first. */
    fun delete() {
        val id = _uiState.value.draft.id
        if (id == 0L) return
        viewModelScope.launch {
            repository.deleteProfile(id)
            _uiState.update { it.copy(done = true) }
        }
    }

    /** Every change makes an earlier test result stale. */
    private fun edit(change: (AiProfileDraft) -> AiProfileDraft) {
        testJob?.cancel()
        _uiState.update { it.copy(draft = change(it.draft), test = ConnectionTestState.Idle) }
    }

    /** Kind, URL or key changed: the model list must be loaded again. */
    private fun editConnection(change: (AiProfileDraft) -> AiProfileDraft) {
        edit(change)
        scheduleModelLoad()
    }

    companion object {
        const val MODEL_DEBOUNCE_MILLIS = 800L
    }
}
