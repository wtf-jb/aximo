package io.github.wtfjb.aximo.domain.routine

import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import kotlin.time.Instant

/** Rules for routines (A-05). Pure functions, fully tested. */
object RoutineLogic {

    /** Default target RIR for a new routine exercise (the mockups use RIR 2). */
    const val DEFAULT_TARGET_RIR = 2

    /** Default number of sets for a new routine exercise. */
    const val DEFAULT_TARGET_SETS = 3

    /**
     * The routine to do next: the one after the most recently trained routine
     * (in list order, wrapping around). Never trained → the first one.
     * [routines] must be in display order; [lastTrained] maps routine id → last start.
     */
    fun nextRoutine(routines: List<Routine>, lastTrained: Map<Long, Instant>): Routine? {
        if (routines.isEmpty()) return null
        val lastId = lastTrained.filterKeys { id -> routines.any { it.id == id } }.maxByOrNull { it.value }?.key
            ?: return routines.first()
        val index = routines.indexOfFirst { it.id == lastId }
        return routines[(index + 1) % routines.size]
    }

    /**
     * Sets to create for a routine exercise when a workout starts: as many as the
     * target says, with weight and reps from the matching working set of the last
     * session (else the last one, else 0 kg at the lower rep target) and the target RIR.
     * A progression suggestion (A-06) replaces weight and reps.
     */
    fun plannedSets(
        target: RoutineExercise,
        lastSession: List<SetEntry>,
        progression: ProgressionState? = null,
    ): List<PlannedSet> {
        val working = lastSession.filter { it.setType != SetType.WARM_UP }
        val sets = List(target.targetSets) { index ->
            val template = working.getOrNull(index) ?: working.lastOrNull()
            PlannedSet(
                weightKg = template?.weightKg ?: 0.0,
                reps = template?.reps ?: target.repMin,
                rir = target.targetRir,
            )
        }
        return WorkoutLogic.applyProgression(sets, progression)
    }
}
