package io.github.wtfjb.aximo.domain.rest

import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import kotlin.time.Instant

/** When a rest starts and what comes next (A-03). Pure functions, fully tested. */
object RestTimerLogic {

    /** "+15 s" on the timer bar and in the notification. */
    const val EXTEND_SECONDS = 15

    /**
     * Rest in seconds to start when the set [setId] of [group] is completed,
     * or null for no rest. [group] is the state before completing.
     *
     * A single exercise rests after every set. A superset rests only once the
     * round is complete (set 1 of every exercise, then set 2 …), and then for
     * the longest rest of its exercises. A rest of 0 seconds means no timer.
     */
    fun restAfterCompleting(group: List<WorkoutExerciseDetail>, setId: Long): Int? {
        val owner = group.firstOrNull { exercise -> exercise.sets.any { it.id == setId } } ?: return null
        val round = owner.sets.indexOfFirst { it.id == setId }
        val roundStillOpen = group.any { exercise ->
            val set = exercise.sets.getOrNull(round)
            set != null && set.id != setId && set.completedAt == null
        }
        if (roundStillOpen) return null
        return group.maxOf { it.exercise.restSeconds }.takeIf { it > 0 }
    }

    /**
     * The set to do after completing [setId]: the next open set of the same
     * group, else the first open set of the following groups, else of the
     * groups before. Null when nothing is left.
     */
    fun nextSetAfterCompleting(groups: List<List<WorkoutExerciseDetail>>, setId: Long): NextSet? {
        val index = groups.indexOfFirst { group -> group.any { exercise -> exercise.sets.any { it.id == setId } } }
        if (index < 0) return null
        val ordered = groups.drop(index) + groups.take(index)
        for (group in ordered) {
            val completed = group.map { exercise ->
                exercise.copy(sets = exercise.sets.map { if (it.id == setId) it.copy(completedAt = Instant.DISTANT_PAST) else it })
            }
            val activeId = WorkoutLogic.activeSetId(completed) ?: continue
            val exercise = completed.first { candidate -> candidate.sets.any { it.id == activeId } }
            val number = WorkoutLogic.setNumbers(exercise.sets)[exercise.sets.indexOfFirst { it.id == activeId }]
            return NextSet(exercise.exercise.name, number)
        }
        return null
    }
}
