package io.github.wtfjb.aximo.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.progression.ProgressionRules
import io.github.wtfjb.aximo.domain.recent.RecentActivity
import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.repository.CardioRepository
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.ProgressionRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.routine.RoutineLogic
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.today.NextWorkout
import io.github.wtfjb.aximo.domain.today.PreviewItem
import io.github.wtfjb.aximo.domain.today.WeekBar
import io.github.wtfjb.aximo.domain.today.WeekOverview
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/** The hero card: the routine to do next with its length and progression preview. */
data class NextWorkoutUi(
    val routine: Routine,
    val exerciseCount: Int,
    val estimatedMinutes: Int,
    val preview: List<PreviewItem>,
)

data class TodayUiState(
    val hasActiveWorkout: Boolean = false,
    /** The routine to do next, or null without routines. */
    val next: NextWorkoutUi? = null,
    /** "Diese Woche", null until loaded. */
    val week: WeekOverview? = null,
    /** "Zuletzt": finished workouts and cardio entries, newest first. */
    val recent: List<RecentItem> = emptyList(),
) {
    val nextRoutine: Routine? get() = next?.routine
}

/**
 * "Heute" tab (mockup Heute.html): next workout with progression preview, the
 * current week and recent activity; resume the running workout or start one.
 */
class TodayViewModel(
    private val workouts: WorkoutRepository,
    routines: RoutineRepository,
    cardio: CardioRepository,
    exercises: ExerciseRepository,
    private val progression: ProgressionRepository,
    private val starter: WorkoutStarter,
    private val time: TimeSource,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val recent = combine(
        workouts.observeRecentFinished(RecentActivity.DEFAULT_LIMIT),
        routines.observeRoutines(),
        cardio.observeRecent(RecentActivity.DEFAULT_LIMIT),
        exercises.observeExercises(includeArchived = true),
    ) { finished, list, entries, all ->
        RecentActivity.merge(finished, list, entries, all)
    }

    private val nextRoutine = combine(
        routines.observeRoutinesWithExercises(),
        workouts.observeLastWorkoutPerRoutine(),
    ) { list, lastTrained ->
        val next = RoutineLogic.nextRoutine(list.map { it.routine }, lastTrained)
        list.firstOrNull { it.routine.id == next?.id }
    }

    // The latest finished workout is only a trigger: suggestions change when a workout ends.
    private val next = combine(
        nextRoutine,
        exercises.observeExercises(includeArchived = true),
        workouts.observeRecentFinished(1),
    ) { routine, all, _ -> routine to all }
        .map { (routine, all) -> routine?.let { nextWorkout(it, all.associateBy { exercise -> exercise.id }) } }

    private val week = combine(workouts.observeFinished(), cardio.observeAll()) { finished, entries ->
        WeekBar.overview(
            strength = finished.map { it.workout.startedAt },
            cardio = entries.map { it.startedAt },
            today = StatsCalendar.localDate(time.now(), zone),
            zone = zone,
        )
    }

    val uiState: StateFlow<TodayUiState> = combine(
        workouts.observeActiveWorkout(),
        next,
        week,
        recent,
    ) { active, nextWorkout, weekOverview, recentItems ->
        TodayUiState(
            hasActiveWorkout = active != null,
            next = nextWorkout,
            week = weekOverview,
            recent = recentItems,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Starts the routine (null = free workout) unless a workout is running, then calls [onReady] to open it. */
    fun startOrResume(routineId: Long?, onReady: () -> Unit) {
        viewModelScope.launch {
            starter.startOrResume(routineId)
            onReady()
        }
    }

    private suspend fun nextWorkout(routine: RoutineWithExercises, exercises: Map<Long, Exercise>): NextWorkoutUi {
        val ids = routine.exercises.map { it.exerciseId }.distinct()
        val suggestions = ids.mapNotNull { progression.get(it) }.associateBy { it.exerciseId }
        val lastTop = ids.mapNotNull { id ->
            ProgressionRules.workingSets(workouts.lastSessionSets(id, excludeWorkoutId = NO_WORKOUT))
                .maxOfOrNull { it.weightKg }
                ?.let { id to it }
        }.toMap()
        return NextWorkoutUi(
            routine = routine.routine,
            exerciseCount = ids.size,
            estimatedMinutes = NextWorkout.estimatedMinutes(routine.exercises, exercises),
            preview = NextWorkout.preview(routine.exercises, exercises, suggestions, lastTop),
        )
    }

    private companion object {
        /** No workout to exclude: ids start at 1. */
        const val NO_WORKOUT = 0L
    }
}
