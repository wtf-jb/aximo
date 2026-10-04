package io.github.wtfjb.aximo.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.exercise.ExerciseFieldError
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the exercise form shows. */
data class ExerciseEditUiState(
    val draft: ExerciseDraft = ExerciseDraft(),
    val isNew: Boolean = true,
    val loading: Boolean = false,
    /** Errors are only shown after the first save attempt, not while typing the first time. */
    val showErrors: Boolean = false,
    /** True once saved or archived: the screen closes. */
    val done: Boolean = false,
) {
    val errors: Set<ExerciseFieldError> get() = if (showErrors) draft.validate() else emptySet()
}

/** Create (exerciseId = 0) or edit an exercise. */
class ExerciseEditViewModel(
    private val repository: ExerciseRepository,
    private val settings: SettingsRepository,
    exerciseId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseEditUiState(isNew = exerciseId == 0L, loading = true))
    val uiState: StateFlow<ExerciseEditUiState> = _uiState.asStateFlow()

    /** The stored version, to keep fields the form doesn't show (rounding step, catalog id). */
    private var original: Exercise? = null

    /** Default steps from the settings, for the increment when the equipment changes. */
    private var steps = TrainingSettings().steps

    init {
        viewModelScope.launch {
            val training = settings.training.first()
            steps = training.steps
            if (exerciseId == 0L) {
                _uiState.update { it.copy(draft = ExerciseDraft.new(training), loading = false) }
            } else {
                val stored = repository.getExercise(exerciseId)
                original = stored
                _uiState.update { state ->
                    state.copy(draft = stored?.let { ExerciseDraft.from(it, training.unit) } ?: state.draft, loading = false)
                }
            }
        }
    }

    fun onEquipmentChange(equipment: Equipment) {
        onDraftChange { it.withEquipment(equipment, steps) }
    }

    /** Applies a change from the form, e.g. `onDraftChange { it.copy(name = text) }`. */
    fun onDraftChange(change: (ExerciseDraft) -> ExerciseDraft) {
        _uiState.update { it.copy(draft = change(it.draft)) }
    }

    fun save() {
        val exercise = _uiState.value.draft.toExercise()
        if (exercise == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        persist(exercise)
    }

    /** Archives or restores an existing exercise and closes the form. */
    fun toggleArchived() {
        onDraftChange { it.copy(archived = !it.archived) }
        save()
    }

    private fun persist(exercise: Exercise) {
        viewModelScope.launch {
            repository.saveExercise(
                exercise.copy(
                    roundingStepKg = original?.roundingStepKg,
                    catalogId = original?.catalogId,
                ),
            )
            _uiState.update { it.copy(done = true) }
        }
    }
}
