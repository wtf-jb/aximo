package io.github.wtfjb.aximo.ui.stats

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Finished workouts with bench press for the statistics ViewModel tests. */
object StatsTestWorkouts {
    val now: Instant = Instant.parse("2026-10-04T12:00:00Z")

    val bench = Exercise(
        id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL,
        primaryMuscles = setOf(MuscleGroup.CHEST), secondaryMuscles = setOf(MuscleGroup.TRICEPS),
    )

    /** A finished workout [daysAgo] days before [now] with one bench set. */
    fun workout(id: Long, daysAgo: Int, weight: Double, reps: Int, routineId: Long? = null): WorkoutDetail {
        val start = now - daysAgo.days
        val set = SetEntry(id = id * 10, workoutExerciseId = id, position = 0, weightKg = weight, reps = reps, completedAt = start)
        return WorkoutDetail(
            workout = Workout(id = id, startedAt = start, endedAt = start + 1.hours, routineId = routineId),
            exercises = listOf(WorkoutExerciseDetail(WorkoutExercise(id, id, bench.id, 0), bench, listOf(set))),
        )
    }
}
