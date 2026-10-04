package io.github.wtfjb.aximo.ui.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.logging.LoggingException
import io.github.wtfjb.aximo.domain.logging.LoggingInput
import io.github.wtfjb.aximo.domain.logging.LoggingProposal
import io.github.wtfjb.aximo.domain.logging.LoggingResult
import io.github.wtfjb.aximo.domain.logging.LoggingService
import io.github.wtfjb.aximo.domain.logging.SpeechError
import io.github.wtfjb.aximo.domain.logging.SpeechEvent
import io.github.wtfjb.aximo.domain.logging.SpeechInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why the last "Auswerten" failed. */
sealed interface LoggingError {
    data class Ai(val reason: AiException.Reason, val statusCode: Int?, val detail: String?) : LoggingError
    data class Logging(val reason: LoggingException.Reason) : LoggingError
}

data class LoggingUiState(
    val text: String = "",
    /** An on-device recognizer exists: the microphone shows. */
    val speechAvailable: Boolean = false,
    /** The recognizer is running (starting or listening). */
    val listening: Boolean = false,
    val speechError: SpeechError? = null,
    /** An AI call is running. */
    val running: Boolean = false,
    /** The preview the user confirms; null while the text is edited. */
    val proposal: LoggingProposal? = null,
    val error: LoggingError? = null,
    /** Notice about the sent data before the first AI call. */
    val showNotice: Boolean = false,
    /** Pretty JSON of what is sent, while the dialog is open. */
    val sentData: String? = null,
    /** Set once saved: the screen closes. */
    val saved: LoggingResult? = null,
)

/**
 * Logging by text or voice (B-04): text field with microphone, AI call, preview.
 * Sets are only saved on the user's tap on "Speichern".
 */
class LoggingViewModel(
    private val service: LoggingService,
    private val preferences: AiPreferences,
    private val speech: SpeechInput,
    /** App language tag, e.g. "de-DE": language of the recognizer and of new exercise names. */
    private val language: () -> String,
    /** Renders the input exactly as it is sent (pretty JSON). */
    private val formatInput: (LoggingInput) -> String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoggingUiState(speechAvailable = speech.isAvailable()))
    val uiState: StateFlow<LoggingUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null

    /** Typing while the recognizer runs would be overwritten by its next result, so it stops first. */
    fun setText(text: String) {
        if (_uiState.value.listening) stopListening()
        _uiState.update { it.copy(text = text.take(LoggingInput.MAX_TEXT), error = null) }
    }

    /** Microphone: starts the recognizer; the recognized text is added to the field and can be corrected. */
    fun startListening() {
        if (listeningJob?.isActive == true || !_uiState.value.speechAvailable) return
        val base = _uiState.value.text.trim()
        _uiState.update { it.copy(listening = true, speechError = null) }
        listeningJob = viewModelScope.launch {
            speech.listen(language()).collect { event ->
                when (event) {
                    SpeechEvent.Listening -> Unit
                    is SpeechEvent.Partial -> setRecognized(base, event.text)
                    is SpeechEvent.Result -> {
                        setRecognized(base, event.text)
                        _uiState.update { it.copy(listening = false) }
                    }
                    is SpeechEvent.Failed -> _uiState.update { it.copy(listening = false, speechError = event.error) }
                }
            }
            // The flow ended without a result (e.g. cancelled).
            _uiState.update { it.copy(listening = false) }
        }
    }

    /** Stops listening; what was recognized so far stays in the field. */
    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        _uiState.update { it.copy(listening = false) }
    }

    /** The user refused the microphone permission. */
    fun speechPermissionDenied() = _uiState.update { it.copy(speechError = SpeechError.PERMISSION) }

    private fun setRecognized(base: String, recognized: String) {
        val text = if (base.isEmpty()) recognized else "$base $recognized"
        _uiState.update { it.copy(text = text.take(LoggingInput.MAX_TEXT)) }
    }

    /** "Auswerten": first time the notice, afterwards straight to the AI. */
    fun requestParse() {
        val state = _uiState.value
        if (state.running) return
        stopListening()
        if (state.text.isBlank()) {
            _uiState.update { it.copy(error = LoggingError.Logging(LoggingException.Reason.EMPTY_TEXT)) }
            return
        }
        viewModelScope.launch {
            if (preferences.dataNoticeAccepted.first()) run() else _uiState.update { it.copy(showNotice = true) }
        }
    }

    fun acceptNotice() {
        _uiState.update { it.copy(showNotice = false) }
        viewModelScope.launch {
            preferences.acceptDataNotice()
            run()
        }
    }

    fun dismissNotice() = _uiState.update { it.copy(showNotice = false) }

    fun showSentData() {
        viewModelScope.launch {
            val text = formatInput(service.input(_uiState.value.text))
            _uiState.update { it.copy(sentData = text) }
        }
    }

    fun dismissSentData() = _uiState.update { it.copy(sentData = null) }

    /** Back from the preview to the text; the text stays. */
    fun discard() = _uiState.update { it.copy(proposal = null, error = null) }

    fun setIncluded(index: Int, included: Boolean) = _uiState.update { it.copy(proposal = it.proposal?.setIncluded(index, included)) }

    fun select(index: Int, option: Int) = _uiState.update { it.copy(proposal = it.proposal?.select(index, option)) }

    fun save() {
        val proposal = _uiState.value.proposal?.takeIf { it.canSave } ?: return
        viewModelScope.launch {
            val result = service.save(proposal)
            _uiState.update { it.copy(saved = result) }
        }
    }

    private suspend fun run() {
        if (_uiState.value.running) return
        _uiState.update { it.copy(running = true, error = null) }
        var proposal: LoggingProposal? = null
        val error = try {
            proposal = service.parse(_uiState.value.text, language())
            null
        } catch (e: AiException) {
            LoggingError.Ai(e.reason, e.statusCode, e.detail)
        } catch (e: LoggingException) {
            LoggingError.Logging(e.reason)
        }
        _uiState.update { it.copy(running = false, error = error, proposal = proposal) }
    }
}
