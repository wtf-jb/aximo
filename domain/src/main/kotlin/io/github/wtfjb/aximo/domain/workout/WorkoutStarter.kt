package io.github.wtfjb.aximo.domain.workout

import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.routine.RoutineLogic
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlinx.coroutines.flow.first

/** Starts a workout, free or from a routine. Never starts a second one while one is running. */
class WorkoutStarter(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val time: TimeSource,
) {
    /** Returns the id of the running workout: the one already running, or a new one. */
    suspend fun startOrResume(routineId: Long? = null): Long {
        workouts.observeActiveWorkout().first()?.let { return it.workout.id }
        val workoutId = workouts.startWorkout(time.now(), routineId)
        val routine = routineId?.let { routines.getRoutine(it) } ?: return workoutId
        for (target in routine.exercises) {
            val last = workouts.lastSessionSets(target.exerciseId, workoutId)
            workouts.addExercise(workoutId, target.exerciseId, target.supersetGroup, RoutineLogic.plannedSets(target, last))
        }
        return workoutId
    }
}
