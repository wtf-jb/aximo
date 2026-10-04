package io.github.wtfjb.aximo.domain.workout

import io.github.wtfjb.aximo.domain.progression.ProgressionRules
import io.github.wtfjb.aximo.domain.repository.ProgressionRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlinx.coroutines.flow.first

/**
 * Ends a workout and runs the progression rules (A-06) for every exercise in it.
 * The rep range comes from the routine target if the workout has a routine, else
 * from the exercise. Weights are rounded to the default step from the settings
 * (A-09) unless the exercise has its own.
 */
class WorkoutFinisher(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val progression: ProgressionRepository,
    private val time: TimeSource,
    private val settings: SettingsRepository,
) {
    suspend fun finish(workoutId: Long, note: String) {
        val steps = settings.training.first().steps
        val detail = workouts.getWorkout(workoutId) ?: return
        workouts.finishWorkout(workoutId, time.now(), note)

        val targets = detail.workout.routineId?.let { routines.getRoutine(it) }?.exercises.orEmpty()
        // An exercise can appear twice in a workout; its sets count together.
        for ((exerciseId, entries) in detail.exercises.groupBy { it.entry.exerciseId }) {
            val exercise = entries.first().exercise
            val target = targets.firstOrNull { it.exerciseId == exerciseId }
            val previous = workouts.lastSessionSets(exerciseId, excludeWorkoutId = workoutId)
            val state = ProgressionRules.evaluate(
                exercise = exercise,
                repMin = target?.repMin ?: exercise.repRangeMin,
                repMax = target?.repMax ?: exercise.repRangeMax,
                current = entries.flatMap { it.sets },
                previous = previous,
                defaultStepKg = steps.forEquipment(exercise.equipment),
            )
            if (state != null) progression.save(state)
        }
    }
}
