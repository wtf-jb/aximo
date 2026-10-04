package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.progression.ProgressionRules
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionRulesTest {

    private val done = Instant.fromEpochSeconds(1_790_000_000)
    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, incrementKg = 2.5)
    private val dips = Exercise(id = 2, name = "Dips", type = ExerciseType.BODYWEIGHT, equipment = Equipment.BODYWEIGHT, incrementKg = 2.5)

    private fun s(weight: Double, reps: Int, rir: Int? = null, type: SetType = SetType.WORKING, completed: Boolean = true) =
        SetEntry(workoutExerciseId = 1, position = 0, weightKg = weight, reps = reps, rir = rir, setType = type, completedAt = if (completed) done else null)

    private fun eval(exercise: Exercise, current: List<SetEntry>, previous: List<SetEntry> = emptyList()) =
        ProgressionRules.evaluate(exercise, repMin = 6, repMax = 8, current = current, previous = previous)

    @Test
    fun allSetsAtTheTopWithRirIncreaseTheWeight() {
        val state = eval(bench, listOf(s(80.0, 8, rir = 1), s(80.0, 8, rir = 2), s(80.0, 9, rir = 1)))!!
        assertEquals(ProgressionReason.INCREASE_WEIGHT, state.reason)
        assertEquals(82.5, state.nextWeightKg, 0.0)
        assertEquals(6, state.nextRepTarget)
    }

    @Test
    fun noRirLoggedCountsAsOk() {
        assertEquals(ProgressionReason.INCREASE_WEIGHT, eval(bench, listOf(s(80.0, 8), s(80.0, 8)))!!.reason)
    }

    @Test
    fun rirZeroAtTheTopHoldsTheWeight() {
        val state = eval(bench, listOf(s(80.0, 8, rir = 0), s(80.0, 8, rir = 1)))!!
        assertEquals(ProgressionReason.HOLD, state.reason)
        assertEquals(80.0, state.nextWeightKg, 0.0)
        assertEquals(8, state.nextRepTarget)
    }

    @Test
    fun rpeIsReadAsRir() {
        assertEquals(ProgressionReason.INCREASE_WEIGHT, eval(bench, listOf(s(80.0, 8).copy(rpe = 9.0), s(80.0, 8).copy(rpe = 8.0)))!!.reason)
        assertEquals(ProgressionReason.HOLD, eval(bench, listOf(s(80.0, 8).copy(rpe = 9.5), s(80.0, 8).copy(rpe = 8.0)))!!.reason)
    }

    @Test
    fun oneSetShortHoldsWithOneMoreRepAsTarget() {
        val state = eval(bench, listOf(s(80.0, 8), s(80.0, 8), s(80.0, 6)))!!
        assertEquals(ProgressionReason.HOLD, state.reason)
        assertEquals(7, state.nextRepTarget)
    }

    @Test
    fun warmUpsAndOpenSetsDoNotCount() {
        val state = eval(bench, listOf(s(40.0, 3, type = SetType.WARM_UP), s(80.0, 8), s(80.0, 2, completed = false)))!!
        assertEquals(ProgressionReason.INCREASE_WEIGHT, state.reason)
        assertNull(eval(bench, listOf(s(40.0, 10, type = SetType.WARM_UP))))
    }

    @Test
    fun belowTwiceInARowSuggestsALowerWeight() {
        val previous = listOf(s(100.0, 5), s(100.0, 4))
        val state = eval(bench, listOf(s(100.0, 5), s(100.0, 5), s(100.0, 6)), previous)!!
        assertEquals(ProgressionReason.DECREASE_WEIGHT, state.reason)
        assertEquals(90.0, state.nextWeightKg, 0.0) // 10 %, rounded to 2.5
        assertEquals(6, state.nextRepTarget)
    }

    @Test
    fun belowOnceOnlyHolds() {
        assertEquals(ProgressionReason.HOLD, eval(bench, listOf(s(100.0, 5), s(100.0, 4)), listOf(s(100.0, 7)))!!.reason)
        assertEquals(ProgressionReason.HOLD, eval(bench, listOf(s(100.0, 5), s(100.0, 4)))!!.reason)
    }

    @Test
    fun lightWeightsDropAtLeastOneIncrement() {
        val state = eval(bench, listOf(s(20.0, 4)), listOf(s(20.0, 4)))!!
        assertEquals(17.5, state.nextWeightKg, 0.0)
    }

    @Test
    fun roundsToThePlateStepAndRespectsOverride() {
        val odd = bench.copy(incrementKg = 2.0)
        assertEquals(82.5, eval(odd, listOf(s(80.0, 8)))!!.nextWeightKg, 0.0) // 82 → 82.5
        val small = bench.copy(incrementKg = 1.0, roundingStepKg = 1.25)
        assertEquals(81.25, eval(small, listOf(s(80.0, 8)))!!.nextWeightKg, 0.0)
        val tiny = bench.copy(incrementKg = 0.5)
        assertEquals(82.5, eval(tiny, listOf(s(80.0, 8)))!!.nextWeightKg, 0.0) // 80.5 rounds to 80 → one step up
    }

    @Test
    fun bodyweightAddsRepsThenWeight() {
        val reps = eval(dips, listOf(s(0.0, 7), s(0.0, 6)))!!
        assertEquals(ProgressionReason.INCREASE_REPS, reps.reason)
        assertEquals(7, reps.nextRepTarget)
        assertEquals(0.0, reps.nextWeightKg, 0.0)

        val weight = eval(dips, listOf(s(0.0, 8), s(0.0, 8)))!!
        assertEquals(ProgressionReason.INCREASE_WEIGHT, weight.reason)
        assertEquals(2.5, weight.nextWeightKg, 0.0)
    }

    @Test
    fun assistedBodyweightLessensTheAssistance() {
        val state = eval(dips, listOf(s(-20.0, 8), s(-20.0, 8)))!!
        assertEquals(-17.5, state.nextWeightKg, 0.0)
    }

    @Test
    fun roundToStepHandlesNegativeAndZeroStep() {
        assertEquals(-17.5, ProgressionRules.roundToStep(-17.4, 2.5), 0.0)
        assertEquals(81.3, ProgressionRules.roundToStep(81.3, 0.0), 0.0)
    }
}
