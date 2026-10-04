package io.github.wtfjb.aximo.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.routine.RoutineDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineEditUiState(
    val draft: RoutineDraft = RoutineDraft(),
    /** All exercises (incl. archived) by id, to show names and rest times. */
    val exercises: Map<Long, Exercise> = emptyMap(),
    val isNew: Boolean = true,
    val loading: Boolean = true,
    /** The missing-name hint only appears after the first save attempt. */
    val showErrors: Boolean = false,
    /** True once saved or deleted: the screen closes. */
    val done: Boolean = false,
)

/** Create (routineId = 0) or edit a routine (A-05, mockup Routine.html). */
class RoutineEditViewModel(
    private val routines: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    routineId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineEditUiState(isNew = routineId == 0L))
    val uiState: StateFlow<RoutineEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercises = exerciseRepository.observeExercises(includeArchived = true).first().associateBy { it.id }
            val draft = if (routineId == 0L) {
                // New routines go to the end of the list.
                val nextPosition = (routines.observeRoutines().first().maxOfOrNull { it.position } ?: -1) + 1
                RoutineDraft(position = nextPosition)
            } else {
                routines.getRoutine(routineId)?.let { RoutineDraft.from(it.routine, it.exercises) } ?: RoutineDraft()
            }
            _uiState.update { it.copy(draft = draft, exercises = exercises, loading = false) }
        }
    }

    private fun change(transform: (RoutineDraft) -> RoutineDraft) {
        _uiState.update { it.copy(draft = transform(it.draft)) }
    }

    fun onNameChange(name: String) = change { it.copy(name = name) }

    /** Adds picked exercises. Reloads the exercise map first, the picker may have created new ones. */
    fun addExercises(ids: List<Long>, superset: Boolean) {
        viewModelScope.launch {
            val exercises = exerciseRepository.observeExercises(includeArchived = true).first().associateBy { it.id }
            _uiState.update { state ->
                state.copy(exercises = exercises, draft = state.draft.add(ids.mapNotNull { exercises[it] }, superset))
            }
        }
    }

    fun move(index: Int, delta: Int) = change { it.move(index, delta) }

    /** Drag and drop: moves a whole card (single exercise or superset). */
    fun moveGroup(from: Int, to: Int) = change { it.moveGroup(from, to) }

    fun remove(index: Int) = change { it.remove(index) }

    fun ungroup(index: Int) = change { it.ungroup(index) }

    fun updateTargets(index: Int, sets: Int, repMin: Int, repMax: Int, rir: Int?) =
        change { it.updateTargets(index, sets, repMin, repMax, rir) }

    fun save() {
        val result = _uiState.value.draft.toRoutine()
        if (result == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            routines.saveRoutine(result.first, result.second)
            _uiState.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val id = _uiState.value.draft.id
        if (id == 0L) return
        viewModelScope.launch {
            routines.deleteRoutine(id)
            _uiState.update { it.copy(done = true) }
        }
    }
}
