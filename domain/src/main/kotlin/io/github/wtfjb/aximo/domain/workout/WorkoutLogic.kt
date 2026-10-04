package io.github.wtfjb.aximo.domain.workout

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType

/**
 * Rules for the logging screen (A-02). Pure functions, no Android, fully tested.
 */
object WorkoutLogic {

    /** Number of sets for an exercise without history. */
    const val DEFAULT_SET_COUNT = 3

    /**
     * Sets to create when an exercise is added: a copy of the last session
     * (types and values), so a set can be logged with one tap. Without history:
     * [DEFAULT_SET_COUNT] working sets at the lower end of the rep range.
     */
    fun initialSets(exercise: Exercise, lastSession: List<SetEntry>, progression: ProgressionState? = null): List<PlannedSet> {
        val sets = if (lastSession.isNotEmpty()) {
            lastSession.map { PlannedSet(it.weightKg, it.reps, it.rir, it.setType) }
        } else {
            List(DEFAULT_SET_COUNT) { PlannedSet(weightKg = 0.0, reps = exercise.repRangeMin) }
        }
        return applyProgression(sets, progression)
    }

    /**
     * Puts the progression suggestion (A-06) into the working sets: suggested weight
     * and rep target. Warm-ups stay as they were. Without a suggestion nothing changes.
     */
    fun applyProgression(sets: List<PlannedSet>, progression: ProgressionState?): List<PlannedSet> {
        if (progression == null) return sets
        return sets.map { set ->
            if (set.setType == SetType.WARM_UP) set else set.copy(weightKg = progression.nextWeightKg, reps = progression.nextRepTarget)
        }
    }

    /**
     * Values for a set added with "+ Satz": the last set of this workout, else
     * the matching set of the last session, else the defaults. Always a working set.
     */
    fun nextSet(exercise: Exercise, current: List<SetEntry>, lastSession: List<SetEntry>): PlannedSet {
        val template = current.lastOrNull() ?: lastSession.getOrNull(current.size) ?: lastSession.lastOrNull()
        return if (template != null) {
            PlannedSet(template.weightKg, template.reps, template.rir)
        } else {
            PlannedSet(weightKg = 0.0, reps = exercise.repRangeMin)
        }
    }

    /**
     * The set to log next in a group (a single exercise or a superset).
     * Supersets go round by round: set 1 of every exercise, then set 2, and so on.
     * Returns null when every set is done.
     */
    fun activeSetId(group: List<WorkoutExerciseDetail>): Long? {
        val rounds = group.maxOfOrNull { it.sets.size } ?: 0
        for (round in 0 until rounds) {
            for (exercise in group) {
                val set = exercise.sets.getOrNull(round) ?: continue
                if (set.completedAt == null) return set.id
            }
        }
        return null
    }

    /**
     * Groups exercises for display: consecutive exercises with the same
     * superset letter form one group, all others stand alone.
     */
    fun groups(exercises: List<WorkoutExerciseDetail>): List<List<WorkoutExerciseDetail>> {
        val result = mutableListOf<MutableList<WorkoutExerciseDetail>>()
        for (exercise in exercises) {
            val group = exercise.entry.supersetGroup
            val last = result.lastOrNull()
            if (group != null && last != null && last.first().entry.supersetGroup == group) {
                last += exercise
            } else {
                result += mutableListOf(exercise)
            }
        }
        return result
    }

    /** First free superset letter: A, B, C … */
    fun nextSupersetGroup(used: Collection<String?>): String =
        ('A'..'Z').map { it.toString() }.first { it !in used }

    /**
     * Number shown in front of each set: warm-ups get 0 (they show a "W" badge),
     * all other sets are counted 1, 2, 3 …
     */
    fun setNumbers(sets: List<SetEntry>): List<Int> {
        var counter = 0
        return sets.map { if (it.setType == SetType.WARM_UP) 0 else ++counter }
    }

    /** Completed non-warm-up sets of the last session, for "Zuletzt 80 × 8 · 8 · 8". */
    fun lastPerformance(lastSession: List<SetEntry>): List<SetEntry> =
        lastSession.filter { it.setType != SetType.WARM_UP && it.completedAt != null }

    /** Elapsed time as "38:12" or "1:02:03". */
    fun formatElapsed(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            "%d:%02d:%02d".format(h, m, s)
        } else {
            "%d:%02d".format(m, s)
        }
    }
}
