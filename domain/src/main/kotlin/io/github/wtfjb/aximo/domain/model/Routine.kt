package io.github.wtfjb.aximo.domain.model

import kotlinx.datetime.LocalDate

/** A training cycle (mesocycle), made of blocks. Logic for it follows in Prio B. */
data class Cycle(
    val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val weeks: Int,
)

/** Part of a cycle, e.g. "Akkumulation" or a deload week. */
data class Block(
    val id: Long = 0,
    val cycleId: Long,
    val name: String,
    val position: Int,
    val weeks: Int,
    val isDeload: Boolean = false,
)

/** A routine like "Push A" (A-05). [position] orders the list on the Pläne screen. */
data class Routine(
    val id: Long = 0,
    val name: String,
    val position: Int = 0,
    val cycleId: Long? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
    }
}

/**
 * One exercise in a routine, with its targets.
 * Exercises with the same [supersetGroup] (e.g. "A") are done as a superset.
 */
data class RoutineExercise(
    val id: Long = 0,
    val routineId: Long = 0,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int? = null,
    val supersetGroup: String? = null,
) {
    init {
        require(targetSets >= 1) { "targetSets must be at least 1" }
        require(repMin in 1..repMax) { "rep range must be 1 ≤ min ≤ max" }
        require(targetRir == null || targetRir >= 0) { "targetRir must not be negative" }
    }
}

/** A routine together with its exercises, ordered by position. */
data class RoutineWithExercises(
    val routine: Routine,
    val exercises: List<RoutineExercise>,
)
