package io.github.wtfjb.aximo.domain.progression

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Rule-based progression (A-06), run locally after every workout.
 *
 * Double progression:
 * - All working sets reach the top of the rep range at RIR ≥ 1 (or no RIR logged)
 *   → next time the weight goes up by the increment.
 * - More than half of the working sets below the bottom of the range, two
 *   sessions in a row → suggest a lower weight (10 %, at least one increment).
 * - Bodyweight: add reps until the top of the range, then add weight.
 * - Otherwise: same weight, one rep more as target (within the range).
 * Weights are rounded to the plate step (default 2.5 kg, per exercise overridable).
 */
object ProgressionRules {

    const val DEFAULT_PLATE_STEP_KG = 2.5

    /** Share of the weight removed when a reduction is suggested. */
    private const val REDUCTION_FACTOR = 0.10

    /** Sets that count: completed working and failure sets (no warm-ups, no drop sets). */
    fun workingSets(sets: List<SetEntry>): List<SetEntry> =
        sets.filter { it.completedAt != null && (it.setType == SetType.WORKING || it.setType == SetType.FAILURE) }

    /** Rounds to the nearest multiple of [step]. */
    fun roundToStep(kg: Double, step: Double): Double {
        if (step <= 0) return kg
        val rounded = (kg / step).roundToLong() * step
        return Math.round(rounded * 1000) / 1000.0
    }

    /**
     * The suggestion for the next session, or null if there were no completed working sets.
     * [current] are the sets of the workout just finished, [previous] those of the session
     * before it; [repMin]/[repMax] come from the routine target or the exercise.
     */
    fun evaluate(
        exercise: Exercise,
        repMin: Int,
        repMax: Int,
        current: List<SetEntry>,
        previous: List<SetEntry>,
        defaultStepKg: Double = DEFAULT_PLATE_STEP_KG,
    ): ProgressionState? {
        val working = workingSets(current)
        if (working.isEmpty()) return null
        val step = exercise.roundingStepKg ?: defaultStepKg
        val weight = working.maxOf { it.weightKg }
        val fewestReps = working.minOf { it.reps }

        val allAtTop = working.all { it.reps >= repMax && (it.rir == null || it.rir >= 1) }
        val previousWorking = workingSets(previous)
        val belowTwice = isBelow(working, repMin) && previousWorking.isNotEmpty() && isBelow(previousWorking, repMin)

        return when {
            allAtTop -> ProgressionState(
                exerciseId = exercise.id,
                nextWeightKg = increased(weight, exercise.incrementKg, step),
                nextRepTarget = repMin,
                reason = ProgressionReason.INCREASE_WEIGHT,
            )
            belowTwice -> ProgressionState(
                exerciseId = exercise.id,
                nextWeightKg = decreased(weight, exercise, step),
                nextRepTarget = repMin,
                reason = ProgressionReason.DECREASE_WEIGHT,
            )
            exercise.type == ExerciseType.BODYWEIGHT -> ProgressionState(
                exerciseId = exercise.id,
                nextWeightKg = weight,
                nextRepTarget = min(repMax, fewestReps + 1),
                reason = ProgressionReason.INCREASE_REPS,
            )
            else -> ProgressionState(
                exerciseId = exercise.id,
                nextWeightKg = weight,
                nextRepTarget = (fewestReps + 1).coerceIn(repMin, repMax),
                reason = ProgressionReason.HOLD,
            )
        }
    }

    /** More than half of the sets under the bottom of the range. */
    private fun isBelow(sets: List<SetEntry>, repMin: Int): Boolean = sets.count { it.reps < repMin } * 2 > sets.size

    /** Weight plus increment, rounded; always strictly more than before. */
    private fun increased(weight: Double, increment: Double, step: Double): Double {
        val candidate = roundToStep(weight + max(increment, 0.0), step)
        return if (candidate > weight) candidate else roundToStep(weight + step, step)
    }

    /** 10 % less, at least one increment (bodyweight: one increment), rounded; always strictly less. */
    private fun decreased(weight: Double, exercise: Exercise, step: Double): Double {
        val drop = if (exercise.type == ExerciseType.BODYWEIGHT) {
            max(exercise.incrementKg, step)
        } else {
            max(max(exercise.incrementKg, step), weight * REDUCTION_FACTOR)
        }
        val candidate = roundToStep(weight - drop, step)
        val result = if (candidate < weight) candidate else roundToStep(weight - step, step)
        // A barbell can't go below zero; bodyweight may (assistance).
        return if (exercise.type == ExerciseType.BODYWEIGHT) result else max(0.0, floor(result * 1000) / 1000)
    }
}
