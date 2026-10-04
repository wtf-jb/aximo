package io.github.wtfjb.aximo.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.routine.RoutineLogic
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import kotlin.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One routine in the Pläne list. */
data class RoutineItem(
    val routine: Routine,
    val exerciseCount: Int,
    /** Body regions of the primary muscles, most frequent first (max. 3). */
    val regions: List<BodyRegion>,
    val lastTrained: Instant?,
    val isNext: Boolean,
)

data class PlansUiState(
    val loading: Boolean = true,
    val routines: List<RoutineItem> = emptyList(),
)

/** "Pläne" tab (A-05, mockup Plaene.html). */
class PlansViewModel(
    routines: RoutineRepository,
    exercises: ExerciseRepository,
    workouts: WorkoutRepository,
    private val starter: WorkoutStarter,
) : ViewModel() {

    val uiState: StateFlow<PlansUiState> = combine(
        routines.observeRoutinesWithExercises(),
        exercises.observeExercises(includeArchived = true),
        workouts.observeLastWorkoutPerRoutine(),
    ) { list, allExercises, lastTrained ->
        val byId = allExercises.associateBy { it.id }
        val next = RoutineLogic.nextRoutine(list.map { it.routine }, lastTrained)
        PlansUiState(
            loading = false,
            routines = list.map { item ->
                val regions = item.exercises
                    .flatMap { byId[it.exerciseId]?.primaryMuscles.orEmpty() }
                    .map { it.region }
                    .groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }
                    .take(3).map { it.key }
                RoutineItem(
                    routine = item.routine,
                    exerciseCount = item.exercises.size,
                    regions = regions,
                    lastTrained = lastTrained[item.routine.id],
                    isNext = item.routine.id == next?.id,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlansUiState())

    /** Starts the routine (or a free workout with null), or resumes a running workout, then opens it. */
    fun start(routineId: Long?, onReady: () -> Unit) {
        viewModelScope.launch {
            starter.startOrResume(routineId)
            onReady()
        }
    }
}
