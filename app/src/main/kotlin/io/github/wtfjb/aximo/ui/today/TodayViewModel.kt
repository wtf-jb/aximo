package io.github.wtfjb.aximo.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.model.Routine
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
)

/** "Heute" tab: resume the running workout, or start the next routine / a free workout. */
class TodayViewModel(
    workouts: WorkoutRepository,
    routines: RoutineRepository,
    private val starter: WorkoutStarter,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        workouts.observeActiveWorkout(),
        routines.observeRoutines(),
        workouts.observeLastWorkoutPerRoutine(),
    ) { active, list, lastTrained ->
        TodayUiState(hasActiveWorkout = active != null, nextRoutine = RoutineLogic.nextRoutine(list, lastTrained))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Starts the routine (null = free workout) unless a workout is running, then calls [onReady] to open it. */
    fun startOrResume(routineId: Long?, onReady: () -> Unit) {
        viewModelScope.launch {
            starter.startOrResume(routineId)
            onReady()
        }
    }
}
