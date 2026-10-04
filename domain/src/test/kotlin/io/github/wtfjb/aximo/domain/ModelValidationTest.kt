package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.Workout
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelValidationTest {

    private fun exercise(
        name: String = "Bankdrücken",
        repRangeMin: Int = 6,
        repRangeMax: Int = 8,
        primary: Set<MuscleGroup> = setOf(MuscleGroup.CHEST),
        secondary: Set<MuscleGroup> = setOf(MuscleGroup.TRICEPS),
    ) = Exercise(
        name = name,
        type = ExerciseType.STRENGTH,
        equipment = Equipment.BARBELL,
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        repRangeMin = repRangeMin,
        repRangeMax = repRangeMax,
    )

    @Test
    fun validExerciseUsesDefaults() {
        val e = exercise()
        assertEquals(2.5, e.incrementKg, 0.0)
        assertEquals(120, e.restSeconds)
        assertEquals(false, e.archived)
    }

    @Test(expected = IllegalArgumentException::class)
    fun exerciseNameMustNotBeBlank() {
        exercise(name = " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun exerciseRepRangeMustBeOrdered() {
        exercise(repRangeMin = 10, repRangeMax = 8)
    }

    @Test(expected = IllegalArgumentException::class)
    fun muscleCannotBePrimaryAndSecondary() {
        exercise(primary = setOf(MuscleGroup.CHEST), secondary = setOf(MuscleGroup.CHEST))
    }

    @Test(expected = IllegalArgumentException::class)
    fun routineExerciseNeedsAtLeastOneSet() {
        RoutineExercise(exerciseId = 1, position = 0, targetSets = 0, repMin = 6, repMax = 8)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setRpeMustBeInRange() {
        SetEntry(workoutExerciseId = 1, position = 0, weightKg = 80.0, reps = 8, rpe = 11.0)
    }

    @Test
    fun bodyweightAssistanceIsANegativeWeight() {
        val set = SetEntry(workoutExerciseId = 1, position = 0, weightKg = -20.0, reps = 8)
        assertEquals(-20.0, set.weightKg, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun workoutCannotEndBeforeItStarts() {
        Workout(startedAt = Instant.fromEpochSeconds(1000), endedAt = Instant.fromEpochSeconds(999))
    }

    @Test(expected = IllegalArgumentException::class)
    fun workoutRatingMustBeOneToFive() {
        Workout(startedAt = Instant.fromEpochSeconds(1000), rating = 6)
    }
}
