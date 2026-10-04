package io.github.wtfjb.aximo.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiConnectionResult
import io.github.wtfjb.aximo.domain.ai.AiConnectionTester
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProfileDraft
import io.github.wtfjb.aximo.domain.ai.AiProfileFieldError
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import kotlinx.coroutines.Job
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

data class AiProfileEditUiState(
    val draft: AiProfileDraft = AiProfileDraft(),
    val loading: Boolean = false,
    /** Errors appear after the first save or test attempt. */
    val showErrors: Boolean = false,
    val test: ConnectionTestState = ConnectionTestState.Idle,
    /** Saved or deleted: the screen closes. */
    val done: Boolean = false,
) {
    val errors: Set<AiProfileFieldError> get() = if (showErrors) draft.errors() else emptySet()
}

/** Create (profileId = 0) or edit a provider profile (B-01). */
class AiProfileEditViewModel(
    private val repository: AiProfileRepository,
    private val tester: AiConnectionTester,
    profileId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiProfileEditUiState(loading = profileId != 0L))
    val uiState: StateFlow<AiProfileEditUiState> = _uiState.asStateFlow()

    private var testJob: Job? = null

    init {
        if (profileId != 0L) {
            viewModelScope.launch {
                val stored = repository.getProfile(profileId)
                _uiState.update { state ->
                    state.copy(draft = stored?.let { AiProfileDraft.from(it) } ?: state.draft, loading = false)
                }
            }
        }
    }

    fun setName(value: String) = edit { it.copy(name = value) }
    fun setKind(kind: AiProviderKind) = edit { it.withKind(kind) }
    fun setBaseUrl(value: String) = edit { it.copy(baseUrl = value) }
    fun setModel(value: String) = edit { it.copy(model = value) }
    fun setApiKey(value: String) = edit { it.copy(apiKey = value, removeKey = false) }

    /** Marks the stored key for removal on save (or undoes that). */
    fun toggleRemoveKey() = edit { it.copy(removeKey = !it.removeKey, apiKey = "") }

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
            val key = when {
                draft.apiKey.isNotBlank() -> draft.apiKey.trim()
                draft.hasStoredKey && !draft.removeKey -> repository.apiKey(draft.id)
                else -> null
            }
            val result = when (val outcome = tester.test(profile, key)) {
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
}
