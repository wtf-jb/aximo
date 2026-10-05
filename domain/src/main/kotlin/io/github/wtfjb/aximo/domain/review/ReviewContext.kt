package io.github.wtfjb.aximo.domain.review

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import kotlinx.datetime.LocalDate

/**
 * Everything the weekly review (B-02) sends to the AI: aggregated numbers, no
 * raw set logs. Weights in kg. Exactly this is shown under "Gesendete Daten ansehen".
 */
data class ReviewContext(
    val today: LocalDate,
    /** Length of the period looked at. */
    val weeks: Int,
    val sessions: Int,
    val sessionsPerWeek: Double,
    val weeklyGoal: Int?,
    val routines: List<RoutineInfo>,
    val exercises: List<ExerciseTrend>,
    val volume: List<RegionVolume>,
    val effort: EffortTrend,
    /** Days since the last finished session; null without sessions. */
    val daysSinceLastSession: Int? = null,
    /** Longest gap in days between two sessions in the period; 0 with fewer than two sessions. */
    val longestBreakDays: Int = 0,
    /** Exercises the AI may add to a routine. */
    val available: List<AvailableExercise> = emptyList(),
) {
    val isEmpty: Boolean get() = sessions == 0
}

/** A routine as it is planned now; suggestions refer to it by ids. */
data class RoutineInfo(
    val routineId: Long,
    val name: String,
    /** Finished sessions of this routine in the period. */
    val sessions: Int,
    val exercises: List<RoutineExerciseInfo>,
)

data class RoutineExerciseInfo(
    val exerciseId: Long,
    val name: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int?,
)

/** Progress of one exercise over the period. */
data class ExerciseTrend(
    val exerciseId: Long,
    val name: String,
    val sessions: Int,
    /** E1RM (kg) for weighted exercises, most reps for bodyweight. */
    val metric: ProgressMetric,
    val first: Double,
    val last: Double,
    val best: Double,
    /** Sessions since the metric last reached a new high; 0 = the last session was the best. */
    val sessionsSinceBest: Int,
    /** Heaviest working set of the last session. */
    val lastTopWeightKg: Double,
    val lastTopReps: Int,
    /** Average reps in reserve of the working sets in the period, null if never logged. */
    val avgRir: Double?,
    /** The last up to [ExerciseSignals.RECENT_VALUES] values of [metric], oldest first, rounded to 0.1. */
    val recent: List<Double> = emptyList(),
    /** Change from [first] to [last] in percent, null if [first] is not positive. */
    val changePct: Double? = null,
    /** How far [last] is below [best] in percent. */
    val dropFromBestPct: Double = 0.0,
    val daysSinceLast: Int = 0,
    val status: TrendStatus = TrendStatus.TOO_FEW_DATA,
    /** The last session is a new high of the period. */
    val newBest: Boolean = false,
    /** Target of the routine of the last session; null for a free workout or if the exercise is no longer in the routine. */
    val repMax: Int? = null,
    val targetRir: Int? = null,
    /** Sessions in a row (from the last) with all working sets at [repMax]; see [ExerciseSignals.sessionsAtRepCeiling]. */
    val sessionsAtRepCeiling: Int = 0,
    /** Average RIR of the working sets of the last sessions, null if never logged. */
    val recentAvgRir: Double? = null,
    /** [recentAvgRir] minus [targetRir]; null without target or logged RIR. */
    val rirVsTarget: Double? = null,
    val effort: EffortStatus? = null,
)

/** An exercise that can be added: not archived, not cardio. */
data class AvailableExercise(val exerciseId: Long, val name: String, val regions: Set<BodyRegion>)

/** Working sets per week for a body region and the target band. */
data class RegionVolume(
    val region: BodyRegion,
    val setsPerWeek: Double,
    val targetMin: Double,
    val targetMax: Double,
    val status: VolumeStatus = VolumeStatus.IN_RANGE,
)

/** Average reps in reserve in the first and second half of the period. */
data class EffortTrend(val avgRirFirstHalf: Double?, val avgRirSecondHalf: Double?)
