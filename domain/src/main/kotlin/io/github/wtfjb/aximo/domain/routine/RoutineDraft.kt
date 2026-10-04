package io.github.wtfjb.aximo.domain.routine

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic

/**
 * A routine while it is being edited. All changes return a new draft; superset
 * letters are cleaned up after every change (a "superset" of one is none).
 */
data class RoutineDraft(
    val id: Long = 0,
    val name: String = "",
    val position: Int = 0,
    val cycleId: Long? = null,
    val entries: List<RoutineExercise> = emptyList(),
) {
    val nameMissing: Boolean get() = name.isBlank()

    /** Adds exercises at the end with default targets; with [superset] (2 or more) they share a new letter. */
    fun add(exercises: List<Exercise>, superset: Boolean): RoutineDraft {
        val group = if (superset && exercises.size > 1) {
            WorkoutLogic.nextSupersetGroup(entries.map { it.supersetGroup })
        } else {
            null
        }
        val added = exercises.map { exercise ->
            RoutineExercise(
                routineId = id,
                exerciseId = exercise.id,
                position = 0,
                targetSets = RoutineLogic.DEFAULT_TARGET_SETS,
                repMin = exercise.repRangeMin,
                repMax = exercise.repRangeMax,
                targetRir = RoutineLogic.DEFAULT_TARGET_RIR,
                supersetGroup = group,
            )
        }
        return withEntries(entries + added)
    }

    /** Moves the entry at [index] one place up (-1) or down (+1). Out of range does nothing. */
    fun move(index: Int, delta: Int): RoutineDraft {
        val target = index + delta
        if (index !in entries.indices || target !in entries.indices) return this
        val list = entries.toMutableList()
        list[index] = entries[target]
        list[target] = entries[index]
        return withEntries(list)
    }

    /**
     * The entries as cards: a single exercise, or all neighbouring entries of one
     * superset together. Each inner list holds entry indices.
     */
    fun groups(): List<List<Int>> {
        val groups = mutableListOf<MutableList<Int>>()
        entries.forEachIndexed { index, entry ->
            val last = groups.lastOrNull()
            if (entry.supersetGroup != null && last != null && entries[last.first()].supersetGroup == entry.supersetGroup) {
                last += index
            } else {
                groups += mutableListOf(index)
            }
        }
        return groups
    }

    /**
     * Moves a whole card (see [groups]) from position [from] to [to], used by
     * drag and drop. A superset moves as a block. Out of range does nothing.
     */
    fun moveGroup(from: Int, to: Int): RoutineDraft {
        val groups = groups()
        if (from !in groups.indices || to !in groups.indices || from == to) return this
        val reordered = groups.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return withEntries(reordered.flatten().map { entries[it] })
    }

    fun remove(index: Int): RoutineDraft =
        if (index in entries.indices) withEntries(entries.filterIndexed { i, _ -> i != index }) else this

    fun updateTargets(index: Int, sets: Int, repMin: Int, repMax: Int, rir: Int?): RoutineDraft {
        if (index !in entries.indices) return this
        val list = entries.toMutableList()
        list[index] = list[index].copy(targetSets = sets, repMin = repMin, repMax = repMax, targetRir = rir)
        return withEntries(list)
    }

    /** Takes the entry out of its superset. */
    fun ungroup(index: Int): RoutineDraft {
        if (index !in entries.indices) return this
        val list = entries.toMutableList()
        list[index] = list[index].copy(supersetGroup = null)
        return withEntries(list)
    }

    /** The routine and its exercises ready to save, or null while the name is missing. */
    fun toRoutine(): Pair<Routine, List<RoutineExercise>>? {
        if (nameMissing) return null
        return Routine(id = id, name = name.trim(), position = position, cycleId = cycleId) to entries
    }

    private fun withEntries(list: List<RoutineExercise>): RoutineDraft =
        copy(entries = normalizeGroups(list).mapIndexed { i, e -> e.copy(position = i) })

    companion object {
        /** Superset letters only stay on runs of at least two neighbours with the same letter. */
        fun normalizeGroups(list: List<RoutineExercise>): List<RoutineExercise> = list.mapIndexed { i, entry ->
            val group = entry.supersetGroup ?: return@mapIndexed entry
            val hasNeighbour = list.getOrNull(i - 1)?.supersetGroup == group || list.getOrNull(i + 1)?.supersetGroup == group
            if (hasNeighbour) entry else entry.copy(supersetGroup = null)
        }

        fun from(routine: Routine, exercises: List<RoutineExercise>) = RoutineDraft(
            id = routine.id,
            name = routine.name,
            position = routine.position,
            cycleId = routine.cycleId,
            entries = exercises.sortedBy { it.position },
        )
    }
}
