package io.github.wtfjb.aximo.domain.review

import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.stats.MuscleVolume
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import io.github.wtfjb.aximo.domain.workout.Effort

/** How an exercise developed over the review period. The first matching rule in [ExerciseSignals.status] wins. */
enum class TrendStatus { TOO_FEW_DATA, RETURNING, REGRESSING, STAGNATING, PROGRESSING, STABLE }

/** Average effort of the last sessions compared to the target RIR of the routine. */
enum class EffortStatus { TOO_HARD, ON_TARGET, TOO_EASY }

/** Weekly sets of a body region compared to the target band (10–20). */
enum class VolumeStatus { BELOW, IN_RANGE, ABOVE }

/**
 * The AI does not judge trends itself: the app classifies them here and sends
 * the label (see docs/decisions.md, "Coach-Signale"). Pure functions.
 */
object ExerciseSignals {
    /** Fewer sessions in the period: no judgement. */
    const val MIN_SESSIONS = 3

    /** Gap in days between the last two sessions from which a drop counts as coming back, not as losing strength. */
    const val RETURNING_GAP_DAYS = 14

    /** Drop of the mean of the last two values below the best value that counts as regression: 5 % for e1RM. */
    const val REGRESSION_E1RM = 0.05

    /** Same for max reps, which are coarser (8 → 7 is already −12 %): 10 %. */
    const val REGRESSION_REPS = 0.10

    /** Regression needs the best value to be at least this many sessions back. */
    const val REGRESSION_MIN_SESSIONS_SINCE_BEST = 2

    /** No new best for this many sessions is stagnation. */
    const val STAGNATION_SESSIONS = 3

    /** Last value at least this factor above the first one is progress (2.5 %, about one plate). */
    const val PROGRESS_FACTOR = 1.025

    /** The last sessions that count for [rirVsTarget]. */
    const val EFFORT_SESSIONS = 3

    /** Average RIR this far below the target (or lower) is too hard. */
    const val TOO_HARD_DIFF = -1.0

    /** Average RIR this far above the target (or higher) is too easy; RIR is often underestimated, so later than too hard. */
    const val TOO_EASY_DIFF = 2.0

    /** A session counts for the rep ceiling only if the average RIR was at least this (when logged). */
    const val CEILING_MIN_RIR = 1.0

    /** Values shown as "recent" in the context. */
    const val RECENT_VALUES = 8

    private const val PERCENT = 100.0
    private const val EPSILON = 1e-9

    /**
     * [values] are the e1RM (or max reps) per session, oldest first.
     * [gapDays] is the distance in days between the last two sessions, null if there are fewer than two.
     */
    fun status(values: List<Double>, metric: ProgressMetric, gapDays: Int?): TrendStatus {
        if (values.size < MIN_SESSIONS) return TrendStatus.TOO_FEW_DATA
        if (gapDays != null && gapDays >= RETURNING_GAP_DAYS) return TrendStatus.RETURNING
        val best = values.max()
        val sinceBest = ReviewContextBuilder.sessionsSinceBest(values)
        val limit = if (metric == ProgressMetric.E1RM) REGRESSION_E1RM else REGRESSION_REPS
        val lastTwo = values.takeLast(2).average()
        if (sinceBest >= REGRESSION_MIN_SESSIONS_SINCE_BEST && lastTwo <= best * (1 - limit) + EPSILON) return TrendStatus.REGRESSING
        if (sinceBest >= STAGNATION_SESSIONS) return TrendStatus.STAGNATING
        if (values.last() >= values.first() * PROGRESS_FACTOR - EPSILON) return TrendStatus.PROGRESSING
        return TrendStatus.STABLE
    }

    /** The last session is a strictly higher value than all before; needs at least two sessions. */
    fun newBest(values: List<Double>): Boolean =
        values.size >= 2 && values.last() > values.dropLast(1).max()

    /** Change from the first to the last value in percent; null if the first value is not positive. */
    fun changePct(values: List<Double>): Double? {
        val first = values.firstOrNull() ?: return null
        if (first <= 0.0) return null
        return (values.last() - first) / first * PERCENT
    }

    /** How far the last value is below the best one in percent (0 if it is the best). */
    fun dropFromBestPct(values: List<Double>): Double {
        val best = values.maxOrNull() ?: return 0.0
        if (best <= 0.0) return 0.0
        return (best - values.last()) / best * PERCENT
    }

    /**
     * Sessions in a row, counted from the last one, in which every working set
     * reached [repMax] and the average RIR (if logged) was at least [CEILING_MIN_RIR].
     * [sessions] are the working sets per session, oldest first.
     */
    fun sessionsAtRepCeiling(sessions: List<List<SetEntry>>, repMax: Int): Int {
        var count = 0
        for (sets in sessions.asReversed()) {
            val allAtCeiling = sets.isNotEmpty() && sets.all { it.reps >= repMax }
            val rir = averageRir(sets)
            if (!allAtCeiling || (rir != null && rir < CEILING_MIN_RIR)) break
            count++
        }
        return count
    }

    /** Average RIR of the working sets of the last [EFFORT_SESSIONS] sessions minus [targetRir]; null without a target or logged RIR. */
    fun rirVsTarget(sessions: List<List<SetEntry>>, targetRir: Int?): Double? {
        if (targetRir == null) return null
        val average = averageRir(sessions.takeLast(EFFORT_SESSIONS).flatten()) ?: return null
        return average - targetRir
    }

    fun effort(rirVsTarget: Double?): EffortStatus? = when {
        rirVsTarget == null -> null
        rirVsTarget <= TOO_HARD_DIFF + EPSILON -> EffortStatus.TOO_HARD
        rirVsTarget >= TOO_EASY_DIFF - EPSILON -> EffortStatus.TOO_EASY
        else -> EffortStatus.ON_TARGET
    }

    fun volumeStatus(setsPerWeek: Double): VolumeStatus = when {
        setsPerWeek < MuscleVolume.TARGET_MIN -> VolumeStatus.BELOW
        setsPerWeek > MuscleVolume.TARGET_MAX -> VolumeStatus.ABOVE
        else -> VolumeStatus.IN_RANGE
    }

    private fun averageRir(sets: List<SetEntry>): Double? =
        sets.mapNotNull { Effort.rir(it) }.takeIf { it.isNotEmpty() }?.average()
}
