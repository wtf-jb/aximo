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
)

/** An exercise that can be added: not archived, not cardio. */
data class AvailableExercise(val exerciseId: Long, val name: String, val regions: Set<BodyRegion>)

/** Working sets per week for a body region and the target band. */
data class RegionVolume(val region: BodyRegion, val setsPerWeek: Double, val targetMin: Double, val targetMax: Double)

/** Average reps in reserve in the first and second half of the period. */
data class EffortTrend(val avgRirFirstHalf: Double?, val avgRirSecondHalf: Double?)
