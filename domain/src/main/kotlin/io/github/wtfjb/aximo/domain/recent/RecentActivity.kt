package io.github.wtfjb.aximo.domain.recent

import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.stats.Records
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import kotlin.time.Instant

/** One row of "Zuletzt" on the Today tab: a finished workout or a cardio entry. */
sealed interface RecentItem {
    val startedAt: Instant

    /** [routineName] is null for a free workout or a deleted routine. */
    data class WorkoutItem(
        val workoutId: Long,
        val routineName: String?,
        override val startedAt: Instant,
        val durationSec: Long,
        val volumeKg: Double,
    ) : RecentItem

    data class CardioItem(val entry: CardioEntry, val activity: Exercise) : RecentItem {
        override val startedAt: Instant get() = entry.startedAt
    }
}

object RecentActivity {
    const val DEFAULT_LIMIT = 5

    /**
     * Finished workouts and cardio entries merged, newest first, at most [limit].
     * Running workouts and entries of unknown activities are left out.
     */
    fun merge(
        workouts: List<WorkoutDetail>,
        routines: List<Routine>,
        cardio: List<CardioEntry>,
        exercises: List<Exercise>,
        limit: Int = DEFAULT_LIMIT,
    ): List<RecentItem> {
        val routineNames = routines.associate { it.id to it.name }
        val exercisesById = exercises.associateBy { it.id }
        val workoutItems = workouts.mapNotNull { detail ->
            val end = detail.workout.endedAt ?: return@mapNotNull null
            RecentItem.WorkoutItem(
                workoutId = detail.workout.id,
                routineName = detail.workout.routineId?.let { routineNames[it] },
                startedAt = detail.workout.startedAt,
                durationSec = (end - detail.workout.startedAt).inWholeSeconds,
                volumeKg = Records.volume(detail.exercises.flatMap { it.sets }),
            )
        }
        val cardioItems = cardio.mapNotNull { entry ->
            exercisesById[entry.exerciseId]?.let { RecentItem.CardioItem(entry, it) }
        }
        return (workoutItems + cardioItems).sortedByDescending { it.startedAt }.take(limit)
    }
}
