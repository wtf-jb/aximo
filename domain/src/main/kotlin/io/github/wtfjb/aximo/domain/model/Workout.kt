package io.github.wtfjb.aximo.domain.model

import kotlin.time.Instant

/** A training session. [endedAt] is null while it is running. */
data class Workout(
    val id: Long = 0,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val routineId: Long? = null,
    val note: String = "",
    /** How it felt, 1–5, asked on the finish screen. */
    val rating: Int? = null,
) {
    init {
        require(rating == null || rating in 1..5) { "rating must be 1–5" }
        require(endedAt == null || endedAt >= startedAt) { "endedAt must not be before startedAt" }
    }
}

/** One exercise inside a workout. */
data class WorkoutExercise(
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val supersetGroup: String? = null,
    val note: String = "",
)

/** Kind of set (A-02). Warm-up sets don't count for volume and records. */
enum class SetType { WARM_UP, WORKING, DROP, FAILURE }

/**
 * One logged set.
 *
 * [weightKg] is the load in kg. For bodyweight exercises it is the extra load:
 * 0 = bodyweight only, positive = added weight, negative = assistance.
 * [completedAt] is null for a set that is planned but not done yet.
 */
data class SetEntry(
    val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null,
    val rir: Int? = null,
    val setType: SetType = SetType.WORKING,
    val completedAt: Instant? = null,
) {
    init {
        require(reps >= 0) { "reps must not be negative" }
        require(rpe == null || rpe in 1.0..10.0) { "rpe must be 1–10" }
        require(rir == null || rir >= 0) { "rir must not be negative" }
    }
}

/** Where a cardio entry comes from. HEALTH_CONNECT is used from Prio C on. */
enum class CardioSource { MANUAL, HEALTH_CONNECT }

/** A cardio session (A-04), alone or as part of a workout. */
data class CardioEntry(
    val id: Long = 0,
    val workoutId: Long? = null,
    val exerciseId: Long,
    val startedAt: Instant,
    val durationSec: Int,
    val distanceM: Double? = null,
    val avgHeartRate: Int? = null,
    val elevationM: Double? = null,
    val note: String = "",
    val source: CardioSource = CardioSource.MANUAL,
    val externalId: String? = null,
) {
    init {
        require(durationSec > 0) { "durationSec must be positive" }
        require(distanceM == null || distanceM >= 0) { "distanceM must not be negative" }
        require(avgHeartRate == null || avgHeartRate > 0) { "avgHeartRate must be positive" }
    }
}

/** Result of the progression rule for the next workout (A-06). */
data class ProgressionState(
    val exerciseId: Long,
    val nextWeightKg: Double,
    val nextRepTarget: Int,
    val reason: ProgressionReason,
)

/** Why the progression rule suggests what it suggests. */
enum class ProgressionReason { INCREASE_WEIGHT, INCREASE_REPS, HOLD, DECREASE_WEIGHT }
