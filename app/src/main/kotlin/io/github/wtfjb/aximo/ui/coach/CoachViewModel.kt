package io.github.wtfjb.aximo.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.review.AiReview
import io.github.wtfjb.aximo.domain.review.AiReviewRepository
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.ReviewContext
import io.github.wtfjb.aximo.domain.review.ReviewException
import io.github.wtfjb.aximo.domain.review.ReviewService
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.SuggestionApplier
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A suggestion with the names the card shows and whether it still fits the routine. */
data class SuggestionItem(
    val suggestion: AiSuggestion,
    val routineName: String?,
    val exerciseName: String?,
    val applicable: Boolean,
    /** Target exercise of "Übung tauschen". */
    val newExerciseName: String? = null,
    /** Routines of a new plan (B-05), null for other changes. */
    val plan: List<PlanRoutineItem>? = null,
)

data class PlanRoutineItem(val name: String, val exercises: List<PlanExerciseItem>)

data class PlanExerciseItem(val name: String, val isNew: Boolean, val sets: Int, val repMin: Int, val repMax: Int, val targetRir: Int?)

/** The card data for a suggestion of the review or the chat. */
internal fun suggestionItem(suggestion: AiSuggestion, routines: List<RoutineWithExercises>, exercises: List<Exercise>): SuggestionItem {
    val change = suggestion.change
    fun name(id: Long) = exercises.firstOrNull { it.id == id }?.name
    val routine = (change as? SuggestionChange.RoutineChange)?.let { c -> routines.firstOrNull { it.routine.id == c.routineId } }
    return SuggestionItem(
        suggestion = suggestion,
        routineName = routine?.routine?.name ?: (change as? SuggestionChange.DeleteRoutine)?.name,
        exerciseName = (change as? SuggestionChange.ExerciseChange)?.let { name(it.exerciseId) },
        applicable = SuggestionApplier.isApplicable(change, routines, exercises),
        newExerciseName = (change as? SuggestionChange.ReplaceExercise)?.let { name(it.newExerciseId) },
        plan = (change as? SuggestionChange.CreatePlan)?.routines?.map { r ->
            PlanRoutineItem(
                r.name,
                r.exercises.map { e ->
                    val ref = e.exercise
                    PlanExerciseItem(
                        name = when (ref) {
                            is PlanExerciseRef.Existing -> name(ref.exerciseId) ?: "–"
                            is PlanExerciseRef.New -> ref.name
                        },
                        isNew = ref is PlanExerciseRef.New,
                        sets = e.sets,
                        repMin = e.repMin,
                        repMax = e.repMax,
                        targetRir = e.targetRir,
                    )
                },
            )
        },
    )
}

/** Why the last "Review erstellen" or chat message failed. */
sealed interface CoachError {
    data class Ai(val reason: AiException.Reason, val statusCode: Int?, val detail: String?) : CoachError
    data class Review(val reason: ReviewException.Reason) : CoachError
}

data class CoachUiState(
    val loading: Boolean = true,
    val review: AiReview? = null,
    val items: List<SuggestionItem> = emptyList(),
    /** A review is being created. */
    val running: Boolean = false,
    val error: CoachError? = null,
    /** Notice about the sent data before the first AI call. */
    val showNotice: Boolean = false,
    /** Pretty JSON of what is sent, while the dialog is open. */
    val sentData: String? = null,
) {
    /** Open suggestions that can be applied, for "Alle übernehmen". */
    val openApplicable: List<SuggestionItem>
        get() = items.filter { it.applicable && it.suggestion.status == SuggestionStatus.OPEN }
}

/** Transient screen state that is not stored anywhere. */
private data class Transient(
    val running: Boolean = false,
    val error: CoachError? = null,
    val showNotice: Boolean = false,
    val sentData: String? = null,
)

/**
 * Coach tab (B-02): the latest weekly review with its suggestions. Applying
 * happens only on the user's tap, one suggestion at a time or "Alle übernehmen".
 */
class CoachViewModel(
    private val service: ReviewService,
    reviews: AiReviewRepository,
    routines: RoutineRepository,
    exercises: ExerciseRepository,
    private val preferences: AiPreferences,
    /** App language tag for the AI's answer, e.g. "de". */
    private val language: () -> String,
    /** Renders the context exactly as it is sent (pretty JSON). */
    private val formatContext: (ReviewContext) -> String,
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<CoachUiState> = combine(
        reviews.observeLatest(),
        routines.observeRoutinesWithExercises(),
        exercises.observeExercises(includeArchived = true),
        transient,
    ) { review, routineList, exerciseList, t ->
        CoachUiState(
            loading = false,
            review = review,
            items = review?.suggestions.orEmpty().map { suggestionItem(it, routineList, exerciseList) },
            running = t.running,
            error = t.error,
            showNotice = t.showNotice,
            sentData = t.sentData,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachUiState())

    /** "Review erstellen": first time the notice, afterwards straight to the AI. */
    fun requestReview() {
        if (transient.value.running) return
        viewModelScope.launch {
            if (preferences.dataNoticeAccepted.first()) run() else transient.update { it.copy(showNotice = true) }
        }
    }

    fun acceptNotice() {
        transient.update { it.copy(showNotice = false) }
        viewModelScope.launch {
            preferences.acceptDataNotice()
            run()
        }
    }

    fun dismissNotice() = transient.update { it.copy(showNotice = false) }

    fun showSentData() {
        viewModelScope.launch {
            val text = formatContext(service.context())
            transient.update { it.copy(sentData = text) }
        }
    }

    fun dismissSentData() = transient.update { it.copy(sentData = null) }

    fun apply(suggestionId: Long) {
        viewModelScope.launch { service.apply(suggestionId) }
    }

    fun discard(suggestionId: Long) {
        viewModelScope.launch { service.discard(suggestionId) }
    }

    /** Applies all open suggestions that fit, in order; each one is checked again before. */
    fun applyAll() {
        val ids = uiState.value.openApplicable.map { it.suggestion.id }
        viewModelScope.launch { ids.forEach { service.apply(it) } }
    }

    private suspend fun run() {
        if (transient.value.running) return
        transient.update { it.copy(running = true, error = null) }
        val error = try {
            service.createReview(language())
            null
        } catch (e: AiException) {
            CoachError.Ai(e.reason, e.statusCode, e.detail)
        } catch (e: ReviewException) {
            CoachError.Review(e.reason)
        }
        transient.update { it.copy(running = false, error = error) }
    }
}
