package io.github.wtfjb.aximo.ui.plangen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.plan.PlanException
import io.github.wtfjb.aximo.domain.plan.PlanGoal
import io.github.wtfjb.aximo.domain.plan.PlanInput
import io.github.wtfjb.aximo.domain.plan.PlanProposal
import io.github.wtfjb.aximo.domain.plan.PlanRequest
import io.github.wtfjb.aximo.domain.plan.PlanService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why the last "Plan erstellen" failed. */
sealed interface PlanError {
    data class Ai(val reason: AiException.Reason, val statusCode: Int?, val detail: String?) : PlanError
    data class Plan(val reason: PlanException.Reason) : PlanError
}

data class PlanGeneratorUiState(
    val request: PlanRequest = PlanRequest(),
    /** An AI call is running. */
    val running: Boolean = false,
    /** The draft the user confirms; null while the form is shown. */
    val proposal: PlanProposal? = null,
    val error: PlanError? = null,
    /** Notice about the sent data before the first AI call. */
    val showNotice: Boolean = false,
    /** Pretty JSON of what is sent, while the dialog is open. */
    val sentData: String? = null,
    /** True once saved: the screen closes. */
    val done: Boolean = false,
)

/**
 * "Mit KI erstellen" (B-03): form, AI call, draft. Routines are only saved on
 * the user's tap on "Speichern".
 */
class PlanGeneratorViewModel(
    private val service: PlanService,
    private val preferences: AiPreferences,
    /** App language tag for the AI's answer, e.g. "de". */
    private val language: () -> String,
    /** Renders the input exactly as it is sent (pretty JSON). */
    private val formatInput: (PlanInput) -> String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanGeneratorUiState())
    val uiState: StateFlow<PlanGeneratorUiState> = _uiState.asStateFlow()

    private fun changeRequest(transform: (PlanRequest) -> PlanRequest) {
        _uiState.update { it.copy(request = transform(it.request)) }
    }

    fun setGoal(goal: PlanGoal) = changeRequest { it.copy(goal = goal) }

    fun setDays(days: Int) = changeRequest { it.copy(daysPerWeek = days) }

    fun setMinutes(minutes: Int) = changeRequest { it.copy(minutes = minutes) }

    /** Switches one equipment on or off; at least one stays on. */
    fun toggleEquipment(equipment: Equipment) = changeRequest { request ->
        val next = if (equipment in request.equipment) request.equipment - equipment else request.equipment + equipment
        if (next.isEmpty()) request else request.copy(equipment = next)
    }

    fun setRestrictions(text: String) = changeRequest { it.copy(restrictions = text.take(PlanRequest.MAX_RESTRICTIONS)) }

    /** "Plan erstellen": first time the notice, afterwards straight to the AI. */
    fun requestPlan() {
        if (_uiState.value.running) return
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
            val text = formatInput(service.input(_uiState.value.request))
            _uiState.update { it.copy(sentData = text) }
        }
    }

    fun dismissSentData() = _uiState.update { it.copy(sentData = null) }

    /** Back from the draft to the form; the wishes stay. */
    fun discard() = _uiState.update { it.copy(proposal = null, error = null) }

    fun removeRoutine(index: Int) = _uiState.update { it.copy(proposal = it.proposal?.without(index)) }

    fun save() {
        val proposal = _uiState.value.proposal?.takeIf { it.routines.isNotEmpty() } ?: return
        viewModelScope.launch {
            service.save(proposal)
            _uiState.update { it.copy(done = true) }
        }
    }

    private suspend fun run() {
        if (_uiState.value.running) return
        _uiState.update { it.copy(running = true, error = null) }
        var proposal: PlanProposal? = null
        val error = try {
            proposal = service.generate(_uiState.value.request, language())
            null
        } catch (e: AiException) {
            PlanError.Ai(e.reason, e.statusCode, e.detail)
        } catch (e: PlanException) {
            PlanError.Plan(e.reason)
        }
        _uiState.update { it.copy(running = false, error = error, proposal = proposal) }
    }
}
