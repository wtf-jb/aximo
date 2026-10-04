package io.github.wtfjb.aximo.domain.calories

import io.github.wtfjb.aximo.domain.cardio.DefaultActivity
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieEstimateTest {

    private val done = Instant.fromEpochSeconds(1_790_000_000)

    private val squat = Exercise(
        name = "Squat", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, primaryMuscles = setOf(MuscleGroup.QUADS),
    )
    private val bench = Exercise(
        name = "Bench", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, primaryMuscles = setOf(MuscleGroup.CHEST),
    )
    private val legPress = Exercise(
        name = "Leg press", type = ExerciseType.STRENGTH, equipment = Equipment.MACHINE, primaryMuscles = setOf(MuscleGroup.QUADS),
    )
    private val pullUp = Exercise(name = "Pull-up", type = ExerciseType.BODYWEIGHT, equipment = Equipment.BODYWEIGHT)

    private fun detail(exercise: Exercise, doneSets: Int, openSets: Int = 0) = WorkoutExerciseDetail(
        entry = WorkoutExercise(workoutId = 1, exerciseId = 1, position = 0),
        exercise = exercise,
        sets = List(doneSets) { SetEntry(workoutExerciseId = 1, position = it, weightKg = 50.0, reps = 8, completedAt = done) } +
            List(openSets) { SetEntry(workoutExerciseId = 1, position = doneSets + it, weightKg = 50.0, reps = 8) },
    )

    @Test
    fun strengthStandardHourAt80KgIs420() {
        // 5 × 3.5 × 80 / 200 × 60 min
        assertEquals(420, CalorieEstimate.strength(3600, listOf(detail(bench, 3)), 80.0))
    }

    @Test
    fun strengthWithoutDoneSetsUsesStandardMet() {
        assertEquals(5.0, CalorieEstimate.strengthMet(emptyList()), 0.0)
        assertEquals(5.0, CalorieEstimate.strengthMet(listOf(detail(squat, 0, openSets = 3))), 0.0)
    }

    @Test
    fun strengthMetDependsOnKindOfExercise() {
        assertEquals(6.0, CalorieEstimate.strengthMet(listOf(detail(squat, 3))), 0.0)
        assertEquals(5.0, CalorieEstimate.strengthMet(listOf(detail(bench, 3))), 0.0)
        assertEquals(3.5, CalorieEstimate.strengthMet(listOf(detail(legPress, 3))), 0.0)
        assertEquals(3.5, CalorieEstimate.strengthMet(listOf(detail(pullUp, 3))), 0.0)
    }

    @Test
    fun strengthMetIsAveragedOverDoneSets() {
        // 3 × 6.0 + 1 × 3.5, the open set doesn't count
        val met = CalorieEstimate.strengthMet(listOf(detail(squat, 3), detail(legPress, 1, openSets = 1)))
        assertEquals(5.375, met, 1e-9)
    }

    @Test
    fun heavyAndLightWorkoutsDiffer() {
        // 6.0 × 1.4 × 60 = 504, 3.5 × 1.4 × 60 = 294
        assertEquals(500, CalorieEstimate.strength(3600, listOf(detail(squat, 5)), 80.0))
        assertEquals(290, CalorieEstimate.strength(3600, listOf(detail(pullUp, 5)), 80.0))
    }

    @Test
    fun strengthCountsAtMostThreeHours() {
        val sets = listOf(detail(bench, 3))
        assertEquals(CalorieEstimate.strength(3 * 3600, sets, 80.0), CalorieEstimate.strength(10 * 3600, sets, 80.0))
    }

    @Test
    fun zeroOrNegativeDurationIsZero() {
        assertEquals(0, CalorieEstimate.strength(0, emptyList(), 80.0))
        assertEquals(0, CalorieEstimate.strength(-60, emptyList(), 80.0))
    }

    @Test
    fun runningMetIsInterpolatedBetweenTableValues() {
        assertEquals(8.3, CalorieEstimate.runningMet(8.0), 1e-9)
        assertEquals(10.5, CalorieEstimate.runningMet(10.8), 1e-9)
        // halfway between 11.3 km/h (11.0) and 12.1 km/h (11.5)
        assertEquals(11.25, CalorieEstimate.runningMet(11.7), 1e-9)
    }

    @Test
    fun runningMetOutsideTableIsClamped() {
        assertEquals(2.8, CalorieEstimate.runningMet(1.0), 0.0)
        assertEquals(19.0, CalorieEstimate.runningMet(25.0), 0.0)
    }

    @Test
    fun runningWithDistanceUsesSpeed() {
        // 10 km in 60 min: MET ≈ 9.99 → × 1.4 × 60 ≈ 839
        assertEquals(840, CalorieEstimate.cardio(DefaultActivity.RUNNING, 3600, 10_000.0, 80.0))
        // 10 km in 50 min = 12 km/h: MET ≈ 11.44 → × 1.4 × 50 ≈ 801
        assertEquals(800, CalorieEstimate.cardio(DefaultActivity.RUNNING, 50 * 60, 10_000.0, 80.0))
    }

    @Test
    fun slowRunningCountsAsWalking() {
        // 5 km/h: MET 3.7 → × 1.4 × 60 = 310.8
        assertEquals(310, CalorieEstimate.cardio(DefaultActivity.RUNNING, 3600, 5_000.0, 80.0))
    }

    @Test
    fun runningWithoutDistanceUsesDefault() {
        // 8.3 × 1.4 × 30 = 348.6
        assertEquals(350, CalorieEstimate.cardio(DefaultActivity.RUNNING, 30 * 60, null, 80.0))
        assertEquals(350, CalorieEstimate.cardio(DefaultActivity.RUNNING, 30 * 60, 0.0, 80.0))
    }

    @Test
    fun cyclingDependsOnSpeed() {
        // 1 h at 15 km/h: 4 × 84
        assertEquals(340, CalorieEstimate.cardio(DefaultActivity.CYCLING, 3600, 15_000.0, 80.0))
        // 1 h at 25 km/h: 10 × 84
        assertEquals(840, CalorieEstimate.cardio(DefaultActivity.CYCLING, 3600, 25_000.0, 80.0))
        // without distance: 6.8 × 84 = 571.2
        assertEquals(570, CalorieEstimate.cardio(DefaultActivity.CYCLING, 3600, null, 80.0))
    }

    @Test
    fun rowingDependsOnPace() {
        // 30 min, 6000 m = 2:30 / 500 m → 7 × 1.4 × 30 = 294
        assertEquals(290, CalorieEstimate.cardio(DefaultActivity.ROWING, 30 * 60, 6_000.0, 80.0))
        // 30 min, 5000 m = 3:00 / 500 m → 4.8 × 42 = 201.6
        assertEquals(200, CalorieEstimate.cardio(DefaultActivity.ROWING, 30 * 60, 5_000.0, 80.0))
        // 30 min, 7500 m = 2:00 / 500 m → 8.5 × 42 = 357
        assertEquals(360, CalorieEstimate.cardio(DefaultActivity.ROWING, 30 * 60, 7_500.0, 80.0))
    }

    @Test
    fun ownAndOtherActivitiesAreModerate() {
        // 4 × 1.4 × 60 = 336
        assertEquals(340, CalorieEstimate.cardio(null, 3600, null, 80.0))
        assertEquals(340, CalorieEstimate.cardio(DefaultActivity.OTHER, 3600, 12_000.0, 80.0))
    }

    @Test
    fun scalesWithBodyWeight() {
        val sets = listOf(detail(bench, 3))
        // 5 × 3.5 × 120 / 200 × 60 = 630, × 40 kg = 210
        assertEquals(630, CalorieEstimate.strength(3600, sets, 120.0))
        assertEquals(210, CalorieEstimate.strength(3600, sets, 40.0))
    }
}
