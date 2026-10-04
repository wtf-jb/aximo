package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.routine.RoutineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineDraftTest {

    private fun ex(id: Long) = Exercise(
        id = id,
        name = "E$id",
        type = ExerciseType.STRENGTH,
        equipment = Equipment.BARBELL,
        repRangeMin = 6,
        repRangeMax = 8,
    )

    private val draft = RoutineDraft(name = "Push A").add(listOf(ex(1)), superset = false).add(listOf(ex(2), ex(3)), superset = true)

    @Test
    fun addUsesExerciseRangeAndDefaults() {
        val e = draft.entries.first()
        assertEquals(3, e.targetSets)
        assertEquals(6, e.repMin)
        assertEquals(8, e.repMax)
        assertEquals(2, e.targetRir)
        assertEquals(listOf(0, 1, 2), draft.entries.map { it.position })
    }

    @Test
    fun supersetGetsALetterOnlyWithTwoOrMore() {
        assertEquals(listOf(null, "A", "A"), draft.entries.map { it.supersetGroup })
        assertEquals(listOf(null), RoutineDraft(name = "X").add(listOf(ex(1)), superset = true).entries.map { it.supersetGroup })
    }

    @Test
    fun moveSwapsNeighboursAndIgnoresTheEdges() {
        val moved = draft.move(0, +1)
        assertEquals(listOf(2L, 1L, 3L), moved.entries.map { it.exerciseId })
        assertEquals(draft, draft.move(0, -1))
        assertEquals(draft, draft.move(2, +1))
    }

    @Test
    fun breakingASupersetApartClearsSingleLetters() {
        // 1 moved between 2 and 3: neither A has a neighbour with A any more.
        val split = draft.move(0, +1)
        assertEquals(listOf(null, null, null), split.entries.map { it.supersetGroup })
    }

    @Test
    fun removingOneOfTwoSupersetMembersClearsTheOther() {
        val removed = draft.remove(2)
        assertEquals(listOf(1L, 2L), removed.entries.map { it.exerciseId })
        assertEquals(listOf<String?>(null, null), removed.entries.map { it.supersetGroup })
    }

    @Test
    fun updateTargetsAndUngroup() {
        val updated = draft.updateTargets(0, sets = 4, repMin = 5, repMax = 6, rir = null).ungroup(1)
        assertEquals(4, updated.entries[0].targetSets)
        assertNull(updated.entries[0].targetRir)
        assertEquals(listOf<String?>(null, null, null), updated.entries.map { it.supersetGroup })
    }

    @Test
    fun saveNeedsAName() {
        assertNull(draft.copy(name = " ").toRoutine())
        val (routine, entries) = draft.copy(name = " Push A ").toRoutine()!!
        assertEquals("Push A", routine.name)
        assertEquals(3, entries.size)
    }

    @Test
    fun groupsKeepSupersetsTogether() {
        assertEquals(listOf(listOf(0), listOf(1, 2)), draft.groups())
    }

    @Test
    fun moveGroupMovesASupersetAsBlock() {
        val moved = draft.moveGroup(1, 0)
        assertEquals(listOf(2L, 3L, 1L), moved.entries.map { it.exerciseId })
        assertEquals(listOf("A", "A", null), moved.entries.map { it.supersetGroup })
        assertEquals(listOf(0, 1, 2), moved.entries.map { it.position })
        assertEquals(listOf(1L, 2L, 3L), moved.moveGroup(0, 1).entries.map { it.exerciseId })
    }

    @Test
    fun moveGroupIgnoresInvalidPositions() {
        assertEquals(draft, draft.moveGroup(0, 0))
        assertEquals(draft, draft.moveGroup(0, 5))
        assertEquals(draft, draft.moveGroup(-1, 0))
    }
}
