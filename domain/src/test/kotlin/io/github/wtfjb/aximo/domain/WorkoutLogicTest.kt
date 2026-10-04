package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutLogicTest {

    private val done = Instant.fromEpochSeconds(1_790_000_000)
    private val bench = Exercise(
        id = 1,
        name = "Bankdrücken",
        type = ExerciseType.STRENGTH,
        equipment = Equipment.BARBELL,
        repRangeMin = 6,
        repRangeMax = 8,
    )

    private fun set(
        id: Long,
        weight: Double = 80.0,
        reps: Int = 8,
        type: SetType = SetType.WORKING,
        completed: Boolean = false,
        rir: Int? = null,
    ) = SetEntry(
        id = id,
        workoutExerciseId = 1,
        position = id.toInt(),
        weightKg = weight,
        reps = reps,
        rir = rir,
        setType = type,
        completedAt = if (completed) done else null,
    )

    private fun detail(id: Long, group: String?, vararg sets: SetEntry) = WorkoutExerciseDetail(
        entry = WorkoutExercise(id = id, workoutId = 1, exerciseId = id, position = id.toInt(), supersetGroup = group),
        exercise = bench.copy(id = id),
        sets = sets.toList(),
    )

    @Test
    fun initialSetsCopyTheLastSession() {
        val last = listOf(set(1, 40.0, 10, SetType.WARM_UP), set(2, 80.0, 8, rir = 2))
        assertEquals(
            listOf(PlannedSet(40.0, 10, null, SetType.WARM_UP), PlannedSet(80.0, 8, 2, SetType.WORKING)),
            WorkoutLogic.initialSets(bench, last),
        )
    }

    @Test
    fun initialSetsWithoutHistoryUseTheRepRange() {
        assertEquals(List(3) { PlannedSet(0.0, 6) }, WorkoutLogic.initialSets(bench, emptyList()))
    }

    @Test
    fun nextSetCopiesTheLastSetOfThisWorkoutAsWorkingSet() {
        val current = listOf(set(1, 40.0, 10, SetType.WARM_UP), set(2, 82.5, 7, rir = 1))
        assertEquals(PlannedSet(82.5, 7, 1), WorkoutLogic.nextSet(bench, current, emptyList()))
        assertEquals(
            PlannedSet(40.0, 10),
            WorkoutLogic.nextSet(bench, listOf(set(1, 40.0, 10, SetType.WARM_UP)), emptyList()),
        )
    }

    @Test
    fun nextSetFallsBackToLastSessionThenDefaults() {
        val last = listOf(set(1, 80.0, 8), set(2, 75.0, 8))
        assertEquals(PlannedSet(80.0, 8), WorkoutLogic.nextSet(bench, emptyList(), last))
        assertEquals(PlannedSet(0.0, 6), WorkoutLogic.nextSet(bench, emptyList(), emptyList()))
    }

    @Test
    fun activeSetIsTheFirstOpenSetOfASingleExercise() {
        val group = listOf(detail(1, null, set(1, completed = true), set(2), set(3)))
        assertEquals(2L, WorkoutLogic.activeSetId(group))
    }

    @Test
    fun supersetGoesRoundByRound() {
        val a1 = detail(1, "A", set(11, completed = true), set(12))
        val a2 = detail(2, "A", set(21), set(22))
        assertEquals(21L, WorkoutLogic.activeSetId(listOf(a1, a2)))

        val a2Done = detail(2, "A", set(21, completed = true), set(22))
        assertEquals(12L, WorkoutLogic.activeSetId(listOf(a1, a2Done)))
    }

    @Test
    fun noActiveSetWhenEverythingIsDone() {
        assertNull(WorkoutLogic.activeSetId(listOf(detail(1, null, set(1, completed = true)))))
        assertNull(WorkoutLogic.activeSetId(emptyList()))
    }

    @Test
    fun consecutiveExercisesWithTheSameLetterFormAGroup() {
        val e = listOf(detail(1, null), detail(2, "A"), detail(3, "A"), detail(4, null), detail(5, "B"))
        assertEquals(listOf(listOf(1L), listOf(2L, 3L), listOf(4L), listOf(5L)), WorkoutLogic.groups(e).map { g -> g.map { it.entry.id } })
    }

    @Test
    fun nextSupersetLetterSkipsUsedOnes() {
        assertEquals("A", WorkoutLogic.nextSupersetGroup(listOf(null)))
        assertEquals("C", WorkoutLogic.nextSupersetGroup(listOf("A", null, "B")))
    }

    @Test
    fun warmUpsAreNotCounted() {
        val sets = listOf(set(1, type = SetType.WARM_UP), set(2), set(3, type = SetType.DROP), set(4))
        assertEquals(listOf(0, 1, 2, 3), WorkoutLogic.setNumbers(sets))
    }

    @Test
    fun lastPerformanceSkipsWarmUpsAndOpenSets() {
        val sets = listOf(set(1, type = SetType.WARM_UP, completed = true), set(2, completed = true), set(3))
        assertEquals(listOf(2L), WorkoutLogic.lastPerformance(sets).map { it.id })
    }

    @Test
    fun formatsElapsedTime() {
        assertEquals("0:00", WorkoutLogic.formatElapsed(0))
        assertEquals("38:12", WorkoutLogic.formatElapsed(38 * 60 + 12))
        assertEquals("1:02:03", WorkoutLogic.formatElapsed(3723))
    }

    @Test
    fun progressionReplacesWorkingSetValuesButKeepsWarmUps() {
        val last = listOf(set(1, 40.0, 10, SetType.WARM_UP), set(2, 80.0, 8), set(3, 80.0, 8))
        val state = io.github.wtfjb.aximo.domain.model.ProgressionState(
            exerciseId = 1, nextWeightKg = 82.5, nextRepTarget = 6,
            reason = io.github.wtfjb.aximo.domain.model.ProgressionReason.INCREASE_WEIGHT,
        )
        val sets = WorkoutLogic.initialSets(bench, last, state)
        assertEquals(listOf(40.0, 82.5, 82.5), sets.map { it.weightKg })
        assertEquals(listOf(10, 6, 6), sets.map { it.reps })
    }
}
