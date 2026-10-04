package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.pullUp
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionApplier
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionApplierTest {

    private val dips = bench.copy(id = 3, name = "Dips")
    private val exercises = listOf(bench, pullUp, dips)
    private val routine = RoutineWithExercises(
        Routine(id = 7, name = "Push A"),
        listOf(
            RoutineExercise(id = 70, routineId = 7, exerciseId = bench.id, position = 0, targetSets = 3, repMin = 6, repMax = 8, targetRir = 2),
            RoutineExercise(id = 71, routineId = 7, exerciseId = dips.id, position = 1, targetSets = 3, repMin = 8, repMax = 12, supersetGroup = "A"),
            RoutineExercise(id = 72, routineId = 7, exerciseId = pullUp.id, position = 2, targetSets = 3, repMin = 5, repMax = 10, supersetGroup = "A"),
        ),
    )

    private fun ok(change: SuggestionChange) = SuggestionApplier.isApplicable(change, routine, exercises)

    @Test
    fun setCount() {
        val change = SuggestionChange.SetCount(7, bench.id, from = 3, to = 4)
        assertTrue(ok(change))
        assertEquals(4, SuggestionApplier.apply(change, routine)[0].targetSets)

        assertFalse("stale from", ok(change.copy(from = 2)))
        assertFalse("no change", ok(change.copy(to = 3)))
        assertFalse("out of range", ok(change.copy(to = 11)))
        assertFalse("not in routine", ok(SuggestionChange.SetCount(7, 99, 3, 4)))
        assertFalse("other routine", SuggestionApplier.isApplicable(change.copy(routineId = 8), routine, exercises))
        assertFalse("routine gone", SuggestionApplier.isApplicable(change, null, exercises))
    }

    @Test
    fun repRange() {
        val change = SuggestionChange.RepRange(7, bench.id, 6, 8, 8, 10)
        assertTrue(ok(change))
        val result = SuggestionApplier.apply(change, routine)[0]
        assertEquals(8, result.repMin)
        assertEquals(10, result.repMax)

        assertFalse(ok(change.copy(toMin = 12)))
        assertFalse(ok(change.copy(fromMax = 10)))
        assertFalse(ok(change.copy(toMin = 0)))
    }

    @Test
    fun targetRir() {
        assertTrue(ok(SuggestionChange.TargetRir(7, bench.id, 2, 1)))
        assertTrue(ok(SuggestionChange.TargetRir(7, bench.id, 2, null)))
        assertTrue(ok(SuggestionChange.TargetRir(7, dips.id, null, 2)))
        assertFalse(ok(SuggestionChange.TargetRir(7, bench.id, 1, 2)))
        assertFalse(ok(SuggestionChange.TargetRir(7, bench.id, 2, 6)))
        assertNull(SuggestionApplier.apply(SuggestionChange.TargetRir(7, bench.id, 2, null), routine)[0].targetRir)
    }

    @Test
    fun addExercise() {
        val legPress = bench.copy(id = 4, name = "Beinpresse")
        val all = exercises + legPress + bench.copy(id = 5, archived = true) + bench.copy(id = 6, type = ExerciseType.CARDIO)
        val change = SuggestionChange.AddExercise(7, legPress.id, sets = 3, repMin = 10, repMax = 12, targetRir = 2)

        assertTrue(SuggestionApplier.isApplicable(change, routine, all))
        val result = SuggestionApplier.apply(change, routine)
        assertEquals(4, result.size)
        assertEquals(legPress.id, result.last().exerciseId)
        assertEquals(3, result.last().position)

        assertFalse("already in routine", SuggestionApplier.isApplicable(change.copy(exerciseId = bench.id), routine, all))
        assertFalse("archived", SuggestionApplier.isApplicable(change.copy(exerciseId = 5), routine, all))
        assertFalse("cardio", SuggestionApplier.isApplicable(change.copy(exerciseId = 6), routine, all))
        assertFalse("unknown", SuggestionApplier.isApplicable(change.copy(exerciseId = 99), routine, all))
        assertFalse("bad range", SuggestionApplier.isApplicable(change.copy(repMin = 13), routine, all))
    }

    @Test
    fun removeExerciseDissolvesSupersetOfOne() {
        val change = SuggestionChange.RemoveExercise(7, dips.id)
        assertTrue(ok(change))

        val result = SuggestionApplier.apply(change, routine)

        assertEquals(listOf(bench.id, pullUp.id), result.map { it.exerciseId })
        assertEquals(listOf(0, 1), result.map { it.position })
        assertNull(result[1].supersetGroup)
    }

    @Test
    fun lastExerciseCannotBeRemoved() {
        val single = RoutineWithExercises(routine.routine, routine.exercises.take(1))
        assertFalse(SuggestionApplier.isApplicable(SuggestionChange.RemoveExercise(7, bench.id), single, exercises))
    }

    @Test
    fun applicableFiltersAndRemovesDuplicates() {
        val good = GeneratedSuggestion(SuggestionChange.SetCount(7, bench.id, 3, 4), "a")
        val result = SuggestionApplier.applicable(
            listOf(
                good,
                good.copy(rationale = "same change again"),
                GeneratedSuggestion(SuggestionChange.SetCount(7, bench.id, 2, 4), "wrong from"),
                GeneratedSuggestion(SuggestionChange.SetCount(99, bench.id, 3, 4), "unknown routine"),
            ),
            listOf(routine),
            exercises,
        )

        assertEquals(listOf(good), result)
    }
}
