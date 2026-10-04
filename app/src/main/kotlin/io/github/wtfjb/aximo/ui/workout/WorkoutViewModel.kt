package io.github.wtfjb.aximo.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One card on the workout screen: a single exercise or a superset. */
data class WorkoutGroupUi(
    val supersetGroup: String?,
    val exercises: List<WorkoutExerciseDetail>,
    /** The set to log next in this group (accent row), or null when all are done. */
    val activeSetId: Long?,
)

data class WorkoutUiState(
    val loading: Boolean = true,
    /** Null once loaded means there is no running workout (finished or discarded): the screen closes. */
    val workout: WorkoutDetail? = null,
    val groups: List<WorkoutGroupUi> = emptyList(),
    val elapsedSeconds: Long = 0,
    /** Completed non-warm-up sets of the last session, per exercise id. */
    val lastPerformance: Map<Long, List<SetEntry>> = emptyMap(),
    val unit: WeightUnit = WeightUnit.KG,
)

/** Which value of a set is being edited. */
enum class SetField { WEIGHT, REPS, RIR }

/** The running workout (A-02). */
class WorkoutViewModel(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val time: TimeSource,
) : ViewModel() {

    /** Last session per exercise id, loaded once per exercise. */
    private val lastSessions = MutableStateFlow<Map<Long, List<SetEntry>>>(emptyMap())

    private val ticker = flow {
        while (true) {
            emit(time.now())
            delay(1_000)
        }
    }

    private val active = workouts.observeActiveWorkout().onEach { workout ->
        if (workout != null) loadMissingLastSessions(workout)
    }

    val uiState: StateFlow<WorkoutUiState> = combine(active, lastSessions, ticker) { workout, last, now ->
        if (workout == null) {
            WorkoutUiState(loading = false)
        } else {
            WorkoutUiState(
                loading = false,
                workout = workout,
                groups = WorkoutLogic.groups(workout.exercises).map { group ->
                    WorkoutGroupUi(group.first().entry.supersetGroup, group, WorkoutLogic.activeSetId(group))
                },
                elapsedSeconds = (now - workout.workout.startedAt).inWholeSeconds,
                lastPerformance = last.mapValues { WorkoutLogic.lastPerformance(it.value) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    private val currentWorkout: WorkoutDetail? get() = uiState.value.workout

    private suspend fun loadMissingLastSessions(workout: WorkoutDetail) {
        val missing = workout.exercises.map { it.entry.exerciseId }.distinct().filter { it !in lastSessions.value }
        if (missing.isEmpty()) return
        val loaded = missing.associateWith { workouts.lastSessionSets(it, workout.workout.id) }
        lastSessions.update { it + loaded }
    }

    /** Adds the picked exercises; with [superset] (and at least two) they share a new superset letter. */
    fun addExercises(exerciseIds: List<Long>, superset: Boolean) {
        val workout = currentWorkout ?: return
        viewModelScope.launch {
            val group = if (superset && exerciseIds.size > 1) {
                WorkoutLogic.nextSupersetGroup(workout.exercises.map { it.entry.supersetGroup })
            } else {
                null
            }
            for (id in exerciseIds) {
                val exercise = exercises.getExercise(id) ?: continue
                val last = workouts.lastSessionSets(id, workout.workout.id)
                workouts.addExercise(workout.workout.id, id, group, WorkoutLogic.initialSets(exercise, last))
            }
        }
    }

    fun addSet(detail: WorkoutExerciseDetail) {
        viewModelScope.launch {
            val last = lastSessions.value[detail.entry.exerciseId].orEmpty()
            workouts.addSet(detail.entry.id, WorkoutLogic.nextSet(detail.exercise, detail.sets, last))
        }
    }

    /** Check button: completes an open set with its current values, or reopens a done one. */
    fun toggleSetDone(set: SetEntry) {
        val completedAt = if (set.completedAt == null) time.now() else null
        update(set.copy(completedAt = completedAt))
    }

    /** Applies a typed value. Invalid input is ignored; an empty RIR clears it. */
    fun editSet(set: SetEntry, field: SetField, text: String) {
        val changed = when (field) {
            SetField.WEIGHT -> ExerciseDraft.parseNumber(text)?.let { set.copy(weightKg = uiState.value.unit.toKg(it)) }
            SetField.REPS -> text.trim().toIntOrNull()?.takeIf { it >= 0 }?.let { set.copy(reps = it) }
            SetField.RIR -> if (text.isBlank()) {
                set.copy(rir = null)
            } else {
                text.trim().toIntOrNull()?.takeIf { it >= 0 }?.let { set.copy(rir = it) }
            }
        }
        if (changed != null) update(changed)
    }

    fun setType(set: SetEntry, type: SetType) = update(set.copy(setType = type))

    fun deleteSet(set: SetEntry) {
        viewModelScope.launch { workouts.deleteSet(set.id) }
    }

    fun removeExercise(entry: WorkoutExercise) {
        viewModelScope.launch { workouts.removeExercise(entry.id) }
    }

    fun setExerciseNote(entry: WorkoutExercise, note: String) {
        viewModelScope.launch { workouts.setExerciseNote(entry.id, note) }
    }

    fun finish(note: String) {
        val workout = currentWorkout ?: return
        viewModelScope.launch { workouts.finishWorkout(workout.workout.id, time.now(), note) }
    }

    fun discard() {
        val workout = currentWorkout ?: return
        viewModelScope.launch { workouts.discardWorkout(workout.workout.id) }
    }

    private fun update(set: SetEntry) {
        viewModelScope.launch { workouts.updateSet(set) }
    }
}
