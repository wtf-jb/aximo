package io.github.wtfjb.aximo.domain.workout

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.model.WorkoutExercise

/** A workout with everything the logging screen needs. Lists are ordered by position. */
data class WorkoutDetail(
    val workout: Workout,
    val exercises: List<WorkoutExerciseDetail>,
)

/** One exercise in a workout, with the exercise itself and its sets. */
data class WorkoutExerciseDetail(
    val entry: WorkoutExercise,
    val exercise: Exercise,
    val sets: List<SetEntry>,
) {
    val completedSets: Int get() = sets.count { it.completedAt != null }
}

/** Values for a set that is about to be created (not yet done). */
data class PlannedSet(
    val weightKg: Double,
    val reps: Int,
    val rir: Int? = null,
    val setType: SetType = SetType.WORKING,
)
