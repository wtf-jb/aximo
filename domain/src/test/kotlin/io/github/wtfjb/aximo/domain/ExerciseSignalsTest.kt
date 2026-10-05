package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.set
import io.github.wtfjb.aximo.domain.review.EffortStatus
import io.github.wtfjb.aximo.domain.review.ExerciseSignals
import io.github.wtfjb.aximo.domain.review.TrendStatus
import io.github.wtfjb.aximo.domain.review.VolumeStatus
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSignalsTest {

    private fun e1rm(vararg values: Double, gap: Int? = 7) = ExerciseSignals.status(values.toList(), ProgressMetric.E1RM, gap)
    private fun reps(vararg values: Double, gap: Int? = 7) = ExerciseSignals.status(values.toList(), ProgressMetric.REPS, gap)

    @Test
    fun tooFewData() {
        assertEquals(TrendStatus.TOO_FEW_DATA, e1rm())
        assertEquals(TrendStatus.TOO_FEW_DATA, e1rm(100.0, 80.0))
        assertEquals(TrendStatus.TOO_FEW_DATA, e1rm(100.0, 90.0, gap = 30))
    }

    @Test
    fun returningFromFourteenDays() {
        val drop = doubleArrayOf(100.0, 98.0, 80.0, 80.0)
        assertEquals(TrendStatus.REGRESSING, e1rm(*drop, gap = 13))
        assertEquals(TrendStatus.RETURNING, e1rm(*drop, gap = 14))
    }

    @Test
    fun regressionAtExactlyFivePercentOfE1rm() {
        // Mean of the last two = 95.0 = best × 0.95.
        assertEquals(TrendStatus.REGRESSING, e1rm(100.0, 98.0, 95.0, 95.0))
        // 4.9 % below the best: only stagnation.
        assertEquals(TrendStatus.STAGNATING, e1rm(100.0, 98.0, 95.1, 95.1))
    }

    @Test
    fun regressionAtTenPercentForReps() {
        assertEquals(TrendStatus.REGRESSING, reps(10.0, 10.0, 9.0, 9.0))
        // 9 % below: not enough for reps.
        assertEquals(TrendStatus.STAGNATING, reps(100.0, 99.0, 91.0, 91.0))
    }

    @Test
    fun regressionNeedsTheBestTwoSessionsBack() {
        // Best was the second-to-last session: sessions_since_best = 1.
        assertEquals(TrendStatus.STABLE, e1rm(100.0, 200.0, 100.0))
        // Two back is enough.
        assertEquals(TrendStatus.REGRESSING, e1rm(100.0, 104.0, 100.0, 92.0))
    }

    /** Screenshot of the bench press: 123.3 → 104.5 kg was called "stagnating", but is a drop of 15 %. */
    @Test
    fun benchPressScreenshotIsRegressing() {
        assertEquals(TrendStatus.REGRESSING, e1rm(110.0, 118.0, 123.3, 115.0, 108.0, 104.5))
    }

    @Test
    fun stagnationBeatsProgress() {
        // Best three sessions ago, last value still 6 % above the first, no real drop.
        assertEquals(TrendStatus.STAGNATING, e1rm(100.0, 110.0, 109.0, 108.9, 108.8))
        assertEquals(TrendStatus.PROGRESSING, e1rm(100.0, 110.0, 109.0, 108.9)) // only two sessions since best
    }

    @Test
    fun progressingFromTwoAndAHalfPercent() {
        assertEquals(TrendStatus.PROGRESSING, e1rm(100.0, 100.0, 102.5))
        assertEquals(TrendStatus.STABLE, e1rm(100.0, 100.0, 102.4))
    }

    @Test
    fun newBestIsStrictAndNeedsTwoSessions() {
        assertTrue(ExerciseSignals.newBest(listOf(100.0, 105.0)))
        assertFalse(ExerciseSignals.newBest(listOf(100.0, 105.0, 105.0)))
        assertFalse(ExerciseSignals.newBest(listOf(100.0, 105.0, 104.0)))
        assertFalse(ExerciseSignals.newBest(listOf(100.0)))
        assertFalse(ExerciseSignals.newBest(emptyList()))
    }

    @Test
    fun changeAndDropInPercent() {
        assertEquals(-5.0, ExerciseSignals.changePct(listOf(100.0, 120.0, 95.0))!!, 1e-9)
        assertNull(ExerciseSignals.changePct(listOf(0.0, 5.0)))
        assertEquals(20.0, ExerciseSignals.dropFromBestPct(listOf(100.0, 120.0, 96.0)), 1e-9)
        assertEquals(0.0, ExerciseSignals.dropFromBestPct(listOf(100.0, 120.0)), 0.0)
    }

    private fun session(reps: Int, rir: Int? = null, sets: Int = 3) = List(sets) { set(80.0, reps).copy(rir = rir) }

    @Test
    fun repCeilingCountsSessionsInARow() {
        val sessions = listOf(session(6), session(8), session(8))
        assertEquals(2, ExerciseSignals.sessionsAtRepCeiling(sessions, repMax = 8))
        assertEquals(0, ExerciseSignals.sessionsAtRepCeiling(sessions, repMax = 9))
    }

    @Test
    fun repCeilingNeedsAllSets() {
        val mixed = listOf(set(80.0, 8), set(80.0, 7), set(80.0, 8))
        assertEquals(0, ExerciseSignals.sessionsAtRepCeiling(listOf(mixed), repMax = 8))
        assertEquals(0, ExerciseSignals.sessionsAtRepCeiling(listOf(emptyList()), repMax = 8))
    }

    @Test
    fun repCeilingBreaksAtRirZeroButNotWithoutLoggedRir() {
        assertEquals(1, ExerciseSignals.sessionsAtRepCeiling(listOf(session(8, rir = 0), session(8, rir = 1)), repMax = 8))
        assertEquals(0, ExerciseSignals.sessionsAtRepCeiling(listOf(session(8, rir = 1), session(8, rir = 0)), repMax = 8))
        // Average RIR counts: (0 + 1 + 2) / 3 = 1.
        val average = listOf(set(80.0, 8).copy(rir = 0), set(80.0, 8).copy(rir = 1), set(80.0, 8).copy(rir = 2))
        assertEquals(1, ExerciseSignals.sessionsAtRepCeiling(listOf(average), repMax = 8))
        assertEquals(2, ExerciseSignals.sessionsAtRepCeiling(listOf(session(8), session(8)), repMax = 8))
    }

    private fun effort(avgRir: Int, target: Int?) = ExerciseSignals.rirVsTarget(listOf(session(8, rir = avgRir)), target)

    @Test
    fun rirVsTargetBoundaries() {
        assertNull(effort(2, target = null))
        assertNull(ExerciseSignals.rirVsTarget(listOf(session(8)), targetRir = 2))
        assertEquals(-1.0, effort(1, target = 2)!!, 0.0)
        assertEquals(EffortStatus.TOO_HARD, ExerciseSignals.effort(-1.0))
        assertEquals(EffortStatus.ON_TARGET, ExerciseSignals.effort(-0.9))
        assertEquals(EffortStatus.ON_TARGET, ExerciseSignals.effort(1.9))
        assertEquals(EffortStatus.TOO_EASY, ExerciseSignals.effort(2.0))
        assertNull(ExerciseSignals.effort(null))
    }

    @Test
    fun rirVsTargetUsesOnlyTheLastThreeSessions() {
        val sessions = listOf(session(8, rir = 5), session(8, rir = 2), session(8, rir = 2), session(8, rir = 2))
        assertEquals(0.0, ExerciseSignals.rirVsTarget(sessions, targetRir = 2)!!, 0.0)
    }

    @Test
    fun volumeStatusBoundaries() {
        assertEquals(VolumeStatus.BELOW, ExerciseSignals.volumeStatus(9.9))
        assertEquals(VolumeStatus.IN_RANGE, ExerciseSignals.volumeStatus(10.0))
        assertEquals(VolumeStatus.IN_RANGE, ExerciseSignals.volumeStatus(20.0))
        assertEquals(VolumeStatus.ABOVE, ExerciseSignals.volumeStatus(20.1))
    }
}
