package io.github.wtfjb.aximo.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.ProgressionRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.rest.NextSet
import io.github.wtfjb.aximo.domain.rest.RestTimerController
import io.github.wtfjb.aximo.domain.rest.RestTimerLogic
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.Effort
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutFinisher
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

/** The running rest as the timer bar shows it. */
data class RestUi(
    val remainingSeconds: Long,
    /** 1 at the start of the rest, 0 at the end. */
    val remainingFraction: Float,
    val next: NextSet?,
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
    /** Running rest (A-03), or null. */
    val rest: RestUi? = null,
    /** Name of the routine the workout was started from, or null for a free workout. */
    val routineName: String? = null,
    /** Routine targets per exercise id (A-05), empty for a free workout. */
    val targets: Map<Long, RoutineExercise> = emptyMap(),
    /** Progression suggestions per exercise id (A-06). */
    val progression: Map<Long, ProgressionState> = emptyMap(),
)

/** Finishing a workout: nothing yet, running (progression is calculated), done (open the summary). */
sealed interface FinishState {
    data object Idle : FinishState
    data object InProgress : FinishState
    data class Done(val workoutId: Long) : FinishState
}

/** What is loaded once per exercise: last session and progression suggestion. */
private data class ExerciseHistory(
    val lastSessions: Map<Long, List<SetEntry>> = emptyMap(),
    val progression: Map<Long, ProgressionState> = emptyMap(),
)

/** Which value of a set is being edited. */
enum class SetField { WEIGHT, REPS, RIR, RPE }

/** The running workout (A-02) with its rest timer (A-03). */
class WorkoutViewModel(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val routines: RoutineRepository,
    private val progressionRepository: ProgressionRepository,
    private val finisher: WorkoutFinisher,
    private val time: TimeSource,
    private val restTimer: RestTimerController,
    settings: SettingsRepository,
    aiProfiles: AiProfileRepository,
) : ViewModel() {

    /** An AI provider profile exists: "Per Text oder Sprache erfassen" shows (B-04). */
    val aiAvailable: StateFlow<Boolean> = aiProfiles.observeProfiles().map { AiProfiles.isAvailable(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Last session and progression per exercise id, loaded once per exercise. */
    private val history = MutableStateFlow(ExerciseHistory())

    private val _finishState = MutableStateFlow<FinishState>(FinishState.Idle)
    val finishState: StateFlow<FinishState> = _finishState.asStateFlow()

    private val ticker = flow {
        while (true) {
            emit(time.now())
            delay(1_000)
        }
    }

    /** The routine of the running workout, loaded once. */
    private val routine = MutableStateFlow<RoutineWithExercises?>(null)

    /** Routine and display unit together, because combine takes at most five flows. */
    private val routineAndUnit = combine(routine, settings.training.map { it.unit }) { routine, unit -> routine to unit }

    private val active = workouts.observeActiveWorkout().onEach { workout ->
        if (workout != null) {
            loadMissingLastSessions(workout)
            loadRoutine(workout.workout.routineId)
        }
    }

    val uiState: StateFlow<WorkoutUiState> = combine(active, history, ticker, restTimer.state, routineAndUnit) { workout, history, now, rest, (routine, unit) ->
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
                lastPerformance = history.lastSessions.mapValues { WorkoutLogic.lastPerformance(it.value) },
                progression = history.progression,
                // Fresh time instead of the last tick: a rest that just started shows its full length.
                rest = rest?.let { timer ->
                    val current = time.now()
                    RestUi(timer.remainingSeconds(current), timer.remainingFraction(current), timer.next)
                },
                routineName = routine?.routine?.name,
                unit = unit,
                targets = routine?.exercises.orEmpty().reversed().associateBy { it.exerciseId },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    private val currentWorkout: WorkoutDetail? get() = uiState.value.workout

    private suspend fun loadRoutine(routineId: Long?) {
        if (routineId == null || routine.value?.routine?.id == routineId) return
        routine.value = routines.getRoutine(routineId)
    }

    private suspend fun loadMissingLastSessions(workout: WorkoutDetail) {
        val missing = workout.exercises.map { it.entry.exerciseId }.distinct().filter { it !in history.value.lastSessions }
        if (missing.isEmpty()) return
        val sessions = missing.associateWith { workouts.lastSessionSets(it, workout.workout.id) }
        val suggestions = missing.mapNotNull { progressionRepository.get(it) }.associateBy { it.exerciseId }
        history.update { it.copy(lastSessions = it.lastSessions + sessions, progression = it.progression + suggestions) }
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
                val sets = WorkoutLogic.initialSets(exercise, last, progressionRepository.get(id))
                workouts.addExercise(workout.workout.id, id, group, sets)
            }
        }
    }

    fun addSet(detail: WorkoutExerciseDetail) {
        viewModelScope.launch {
            val last = history.value.lastSessions[detail.entry.exerciseId].orEmpty()
            workouts.addSet(detail.entry.id, WorkoutLogic.nextSet(detail.exercise, detail.sets, last))
        }
    }

    /**
     * Check button: completes an open set with its current values, or reopens a done one.
     * Completing starts the rest; in a superset only after the last exercise of the round.
     */
    fun toggleSetDone(set: SetEntry) {
        val completing = set.completedAt == null
        update(set.copy(completedAt = if (completing) time.now() else null))
        if (completing) startRest(set.id)
    }

    private fun startRest(setId: Long) {
        val workout = currentWorkout ?: return
        val groups = WorkoutLogic.groups(workout.exercises)
        val group = groups.firstOrNull { g -> g.any { exercise -> exercise.sets.any { it.id == setId } } } ?: return
        val seconds = RestTimerLogic.restAfterCompleting(group, setId) ?: return
        restTimer.start(seconds, RestTimerLogic.nextSetAfterCompleting(groups, setId))
    }

    fun extendRest() = restTimer.extend()

    fun skipRest() = restTimer.stop()

    /** Applies a typed value. Invalid input is ignored; an empty RIR or RPE clears it. */
    fun editSet(set: SetEntry, field: SetField, text: String) {
        val changed = when (field) {
            SetField.WEIGHT -> ExerciseDraft.parseNumber(text)?.let { set.copy(weightKg = uiState.value.unit.toKg(it)) }
            SetField.REPS -> text.trim().toIntOrNull()?.takeIf { it >= 0 }?.let { set.copy(reps = it) }
            SetField.RIR -> if (text.isBlank()) {
                Effort.withRir(set, null)
            } else {
                text.trim().toIntOrNull()?.takeIf { it >= 0 }?.let { Effort.withRir(set, it) }
            }
            SetField.RPE -> if (text.isBlank()) {
                Effort.withRpe(set, null)
            } else {
                Effort.parseRpe(text)?.let { Effort.withRpe(set, it) }
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

    /** Ends the workout, runs the progression rules and then reports [FinishState.Done]. */
    fun finish() {
        val workout = currentWorkout ?: return
        if (_finishState.value != FinishState.Idle) return
        restTimer.stop()
        _finishState.value = FinishState.InProgress
        viewModelScope.launch {
            finisher.finish(workout.workout.id, workout.workout.note)
            _finishState.value = FinishState.Done(workout.workout.id)
        }
    }

    fun discard() {
        val workout = currentWorkout ?: return
        restTimer.stop()
        viewModelScope.launch { workouts.discardWorkout(workout.workout.id) }
    }

    private fun update(set: SetEntry) {
        viewModelScope.launch { workouts.updateSet(set) }
    }
}
