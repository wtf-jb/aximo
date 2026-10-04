package io.github.wtfjb.aximo.domain.stats

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail

/**
 * Working sets per body region and week (A-07). A set counts 1 for a region
 * with a primary muscle of the exercise, 0.5 if the region has only secondary
 * muscles. Warm-ups don't count.
 */
object MuscleVolume {
    const val PRIMARY = 1.0
    const val SECONDARY = 0.5

    /** Target band per week, from the mockup ("Zielbereich 10–20"). */
    const val TARGET_MIN = 10.0
    const val TARGET_MAX = 20.0

    /** Upper end of the bar scale. */
    const val SCALE_MAX = 24.0

    /** Display order, as in the mockup. */
    val regions = listOf(
        BodyRegion.CHEST,
        BodyRegion.BACK,
        BodyRegion.SHOULDERS,
        BodyRegion.LEGS,
        BodyRegion.ARMS,
        BodyRegion.CORE,
    )

    /** How much one set of [exercise] counts for [region]. */
    fun weight(exercise: Exercise, region: BodyRegion): Double = when {
        exercise.primaryMuscles.any { it.region == region } -> PRIMARY
        exercise.secondaryMuscles.any { it.region == region } -> SECONDARY
        else -> 0.0
    }

    /** Sets per region over all finished [workouts]. Every region is in the map, 0 if untrained. */
    fun setsPerRegion(workouts: List<WorkoutDetail>): Map<BodyRegion, Double> {
        val totals = regions.associateWith { 0.0 }.toMutableMap()
        workouts.filter { it.workout.endedAt != null }.forEach { detail ->
            detail.exercises.filter { it.exercise.type != ExerciseType.CARDIO }.forEach { entry ->
                val sets = Records.countingSets(entry.sets).size
                if (sets == 0) return@forEach
                regions.forEach { region -> totals[region] = totals.getValue(region) + sets * weight(entry.exercise, region) }
            }
        }
        return totals
    }

    /** Average sets per week. */
    fun perWeek(totals: Map<BodyRegion, Double>, weeks: Int): Map<BodyRegion, Double> =
        totals.mapValues { it.value / weeks.coerceAtLeast(1) }

    fun belowTarget(setsPerWeek: Double): Boolean = setsPerWeek < TARGET_MIN
}
