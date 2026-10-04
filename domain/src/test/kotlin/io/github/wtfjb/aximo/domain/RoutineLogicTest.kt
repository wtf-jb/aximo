package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.routine.RoutineLogic
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineLogicTest {

    private val push = Routine(id = 1, name = "Push", position = 0)
    private val pull = Routine(id = 2, name = "Pull", position = 1)
    private val legs = Routine(id = 3, name = "Legs", position = 2)
    private val all = listOf(push, pull, legs)
    private fun t(s: Long) = Instant.fromEpochSeconds(s)

    @Test
    fun noRoutinesMeansNoNext() {
        assertNull(RoutineLogic.nextRoutine(emptyList(), emptyMap()))
    }

    @Test
    fun neverTrainedStartsWithTheFirst() {
        assertEquals(push, RoutineLogic.nextRoutine(all, emptyMap()))
    }

    @Test
    fun nextFollowsTheMostRecentAndWrapsAround() {
        assertEquals(legs, RoutineLogic.nextRoutine(all, mapOf(1L to t(100), 2L to t(200))))
        assertEquals(push, RoutineLogic.nextRoutine(all, mapOf(3L to t(300), 1L to t(100))))
    }

    @Test
    fun deletedRoutinesInHistoryAreIgnored() {
        assertEquals(pull, RoutineLogic.nextRoutine(all, mapOf(99L to t(500), 1L to t(100))))
    }

    private val target = RoutineExercise(exerciseId = 5, position = 0, targetSets = 3, repMin = 6, repMax = 8, targetRir = 2)

    private fun set(weight: Double, reps: Int, type: SetType = SetType.WORKING) =
        SetEntry(workoutExerciseId = 1, position = 0, weightKg = weight, reps = reps, setType = type)

    @Test
    fun plannedSetsWithoutHistoryUseTheTargets() {
        assertEquals(List(3) { PlannedSet(0.0, 6, 2) }, RoutineLogic.plannedSets(target, emptyList()))
    }

    @Test
    fun plannedSetsTakeWorkingSetsFromTheLastSession() {
        val last = listOf(set(40.0, 10, SetType.WARM_UP), set(80.0, 8), set(80.0, 7))
        assertEquals(
            listOf(PlannedSet(80.0, 8, 2), PlannedSet(80.0, 7, 2), PlannedSet(80.0, 7, 2)),
            RoutineLogic.plannedSets(target, last),
        )
    }
}
