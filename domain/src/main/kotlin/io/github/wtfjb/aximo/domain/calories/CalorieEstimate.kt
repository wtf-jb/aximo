package io.github.wtfjb.aximo.domain.calories

import io.github.wtfjb.aximo.domain.cardio.CardioMath
import io.github.wtfjb.aximo.domain.cardio.DefaultActivity
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Estimate of the calories a session burns, based on METs from the Compendium of
 * Physical Activities: kcal/min = MET × 3.5 × body weight (kg) / 200.
 *
 * Gross value (resting metabolism included), like fitness watches show it.
 * Heart rate and elevation are ignored on purpose: for strength training the heart
 * rate overestimates the oxygen uptake a lot. Results are rounded to whole 10 kcal.
 */
object CalorieEstimate {

    /**
     * Strength training over the whole workout including rests. Without a set timer
     * one MET for the whole session (rests are already part of it), averaged over the
     * done sets by kind of exercise; see [strengthMet].
     */
    fun strength(durationSec: Long, exercises: List<WorkoutExerciseDetail>, bodyWeightKg: Double): Int {
        // A workout left running by mistake should not count for hours.
        val seconds = min(durationSec, MAX_STRENGTH_SECONDS).coerceAtLeast(0)
        return kcal(strengthMet(exercises), seconds, bodyWeightKg)
    }

    /**
     * Average MET of the done sets: 3.5 for bodyweight, machines, cables and bands,
     * 6.0 for heavy barbell work on legs or lower back (squat, deadlift), 5.0 for
     * other free weights. Without done sets the standard value 5.0.
     */
    fun strengthMet(exercises: List<WorkoutExerciseDetail>): Double {
        val perSet = exercises.flatMap { detail ->
            val met = setMet(detail.exercise)
            detail.sets.filter { it.completedAt != null }.map { met }
        }
        return if (perSet.isEmpty()) STANDARD_STRENGTH_MET else perSet.average()
    }

    private fun setMet(exercise: Exercise): Double = when {
        exercise.type == ExerciseType.BODYWEIGHT -> LIGHT_STRENGTH_MET
        exercise.equipment in LIGHT_EQUIPMENT -> LIGHT_STRENGTH_MET
        exercise.equipment == Equipment.BARBELL && exercise.primaryMuscles.any { it in HEAVY_MUSCLES } -> HEAVY_STRENGTH_MET
        else -> STANDARD_STRENGTH_MET
    }

    /**
     * A cardio session. [activity] is null for the user's own activities, which
     * count as moderate general activity.
     */
    fun cardio(activity: DefaultActivity?, durationSec: Int, distanceM: Double?, bodyWeightKg: Double): Int {
        val seconds = durationSec.toLong().coerceAtLeast(0)
        val speed = distanceM?.takeIf { it > 0 }?.let { CardioMath.speedKmh(durationSec, it) }
        val met = when (activity) {
            DefaultActivity.RUNNING -> speed?.let(::runningMet) ?: RUNNING_DEFAULT_MET
            DefaultActivity.CYCLING -> speed?.let(::cyclingMet) ?: CYCLING_DEFAULT_MET
            DefaultActivity.ROWING -> rowingMet(durationSec, distanceM)
            DefaultActivity.OTHER, null -> OTHER_MET
        }
        return kcal(met, seconds, bodyWeightKg)
    }

    /**
     * Walking and running by speed, linearly interpolated between the Compendium
     * values; below and above the table the first or last value.
     */
    fun runningMet(speedKmh: Double): Double {
        val first = RUNNING_TABLE.first()
        val last = RUNNING_TABLE.last()
        if (speedKmh <= first.first) return first.second
        if (speedKmh >= last.first) return last.second
        val upperIndex = RUNNING_TABLE.indexOfFirst { it.first >= speedKmh }
        val (lowSpeed, lowMet) = RUNNING_TABLE[upperIndex - 1]
        val (highSpeed, highMet) = RUNNING_TABLE[upperIndex]
        return lowMet + (highMet - lowMet) * (speedKmh - lowSpeed) / (highSpeed - lowSpeed)
    }

    private fun cyclingMet(speedKmh: Double): Double = when {
        speedKmh < 16 -> 4.0
        speedKmh < 19 -> 6.8
        speedKmh < 22.5 -> 8.0
        speedKmh < 25.7 -> 10.0
        else -> 12.0
    }

    /** By pace per 500 m on the rowing machine; without a distance a moderate value. */
    private fun rowingMet(durationSec: Int, distanceM: Double?): Double {
        val pace = distanceM?.let { CardioMath.paceSecondsPer500m(durationSec, it) } ?: return ROWING_DEFAULT_MET
        return when {
            pace > 150 -> 4.8
            pace > 125 -> 7.0
            else -> 8.5
        }
    }

    /** MET × 3.5 × kg / 200 per minute. */
    private fun kcal(met: Double, seconds: Long, bodyWeightKg: Double): Int {
        val minutes = seconds / SECONDS_PER_MINUTE
        val total = met * OXYGEN_ML_PER_KG_MIN * bodyWeightKg / KCAL_DIVISOR * minutes
        return (total.coerceAtLeast(0.0) / 10).roundToInt() * 10
    }

    private const val LIGHT_STRENGTH_MET = 3.5
    private const val STANDARD_STRENGTH_MET = 5.0
    private const val HEAVY_STRENGTH_MET = 6.0
    private val LIGHT_EQUIPMENT = setOf(Equipment.MACHINE, Equipment.CABLE, Equipment.BAND, Equipment.BODYWEIGHT)
    private val HEAVY_MUSCLES = setOf(
        MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK,
    )

    /** Walking (up to 6.4 km/h) and running, km/h to MET (Compendium, mph converted). */
    private val RUNNING_TABLE = listOf(
        3.2 to 2.8,
        4.0 to 3.0,
        4.8 to 3.5,
        5.6 to 4.3,
        6.4 to 5.0,
        8.0 to 8.3,
        8.4 to 9.0,
        9.7 to 9.8,
        10.8 to 10.5,
        11.3 to 11.0,
        12.1 to 11.5,
        12.9 to 11.8,
        13.8 to 12.3,
        14.5 to 12.8,
        16.1 to 14.5,
        17.7 to 16.0,
        19.3 to 19.0,
    )

    private const val RUNNING_DEFAULT_MET = 8.3
    private const val CYCLING_DEFAULT_MET = 6.8
    private const val ROWING_DEFAULT_MET = 7.0
    private const val OTHER_MET = 4.0

    private const val OXYGEN_ML_PER_KG_MIN = 3.5
    private const val KCAL_DIVISOR = 200.0
    private const val MAX_STRENGTH_SECONDS = 3 * 3600L
    private const val SECONDS_PER_MINUTE = 60.0
}
