package io.github.wtfjb.aximo.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.recent.RecentActivity
import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.repository.CardioRepository
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.routine.RoutineLogic
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val hasActiveWorkout: Boolean = false,
    /** The routine to do next, or null without routines. */
    val nextRoutine: Routine? = null,
    /** "Zuletzt": finished workouts and cardio entries, newest first. */
    val recent: List<RecentItem> = emptyList(),
)

/** "Heute" tab: resume the running workout, or start the next routine / a free workout; recent activity. */
class TodayViewModel(
    workouts: WorkoutRepository,
    routines: RoutineRepository,
    cardio: CardioRepository,
    exercises: ExerciseRepository,
    private val starter: WorkoutStarter,
) : ViewModel() {

    private val recent = combine(
        workouts.observeRecentFinished(RecentActivity.DEFAULT_LIMIT),
        routines.observeRoutines(),
        cardio.observeRecent(RecentActivity.DEFAULT_LIMIT),
        exercises.observeExercises(includeArchived = true),
    ) { finished, list, entries, all ->
        RecentActivity.merge(finished, list, entries, all)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        workouts.observeActiveWorkout(),
        routines.observeRoutines(),
        workouts.observeLastWorkoutPerRoutine(),
        recent,
    ) { active, list, lastTrained, recentItems ->
        TodayUiState(
            hasActiveWorkout = active != null,
            nextRoutine = RoutineLogic.nextRoutine(list, lastTrained),
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
}
