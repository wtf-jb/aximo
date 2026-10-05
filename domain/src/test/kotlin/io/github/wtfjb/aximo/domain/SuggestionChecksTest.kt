package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.pullUp
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.review.EffortStatus
import io.github.wtfjb.aximo.domain.review.EffortTrend
import io.github.wtfjb.aximo.domain.review.ExerciseTrend
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.RegionVolume
import io.github.wtfjb.aximo.domain.review.ReviewContext
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionChecks
import io.github.wtfjb.aximo.domain.review.TrendStatus
import io.github.wtfjb.aximo.domain.review.VolumeStatus
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionChecksTest {

    private val exercises = listOf(bench, pullUp)

    private fun trend(exercise: io.github.wtfjb.aximo.domain.model.Exercise, status: TrendStatus, effort: EffortStatus? = null) = ExerciseTrend(
        exerciseId = exercise.id, name = exercise.name, sessions = 6, metric = ProgressMetric.E1RM,
        first = 110.0, last = 104.5, best = 123.3, sessionsSinceBest = 3, lastTopWeightKg = 82.5, lastTopReps = 8, avgRir = 2.0,
        status = status, effort = effort,
    )

    private fun context(
        chest: VolumeStatus,
        back: VolumeStatus = VolumeStatus.IN_RANGE,
        trends: List<ExerciseTrend> = emptyList(),
    ) = ReviewContext(
        today = LocalDate(2026, 10, 4), weeks = 6, sessions = 12, sessionsPerWeek = 2.0, weeklyGoal = 3,
        routines = emptyList(), exercises = trends,
        volume = listOf(
            RegionVolume(BodyRegion.CHEST, 7.8, 10.0, 20.0, chest),
            RegionVolume(BodyRegion.BACK, 12.0, 10.0, 20.0, back),
        ),
        effort = EffortTrend(null, null),
    )

    private fun suggestion(change: SuggestionChange) = GeneratedSuggestion(change, "weil")

    private fun check(change: SuggestionChange, context: ReviewContext) =
        SuggestionChecks.plausible(listOf(suggestion(change)), context, exercises).size

    /** Screenshot: chest 7.8 of 10–20 sets, status stagnating, yet "4 → 3 sets". */
    @Test
    fun screenshotCaseIsDropped() {
        val ctx = context(VolumeStatus.BELOW, trends = listOf(trend(bench, TrendStatus.STAGNATING)))
        assertEquals(0, check(SuggestionChange.SetCount(7, bench.id, from = 4, to = 3), ctx))
    }

    @Test
    fun fewerSetsAreFineWhenVolumeIsNotBelow() {
        assertEquals(1, check(SuggestionChange.SetCount(7, bench.id, 4, 3), context(VolumeStatus.IN_RANGE)))
        assertEquals(1, check(SuggestionChange.SetCount(7, bench.id, 4, 3), context(VolumeStatus.ABOVE)))
    }

    @Test
    fun fewerSetsAreAllowedWhenRegressingOrTooHard() {
        val regressing = context(VolumeStatus.BELOW, trends = listOf(trend(bench, TrendStatus.REGRESSING)))
        val tooHard = context(VolumeStatus.BELOW, trends = listOf(trend(bench, TrendStatus.STABLE, EffortStatus.TOO_HARD)))
        val onTarget = context(VolumeStatus.BELOW, trends = listOf(trend(bench, TrendStatus.STABLE, EffortStatus.ON_TARGET)))
        assertEquals(1, check(SuggestionChange.SetCount(7, bench.id, 4, 3), regressing))
        assertEquals(1, check(SuggestionChange.SetCount(7, bench.id, 4, 3), tooHard))
        assertEquals(0, check(SuggestionChange.SetCount(7, bench.id, 4, 3), onTarget))
    }

    @Test
    fun belowInASecondaryRegionDoesNotCount() {
        // Pull-up: primary back; chest below does not matter.
        assertEquals(1, check(SuggestionChange.SetCount(7, pullUp.id, 4, 3), context(VolumeStatus.BELOW)))
    }

    @Test
    fun moreSetsAreDroppedWhenAboveTarget() {
        assertEquals(0, check(SuggestionChange.SetCount(7, bench.id, 3, 4), context(VolumeStatus.ABOVE)))
        assertEquals(1, check(SuggestionChange.SetCount(7, bench.id, 3, 4), context(VolumeStatus.IN_RANGE)))
        assertEquals(1, check(SuggestionChange.SetCount(7, bench.id, 3, 4), context(VolumeStatus.BELOW)))
    }

    @Test
    fun addExerciseIsDroppedOnlyWhenAllRegionsAreAbove() {
        val add = SuggestionChange.AddExercise(7, bench.id, 3, 8, 12, null)
        assertEquals(0, check(add, context(VolumeStatus.ABOVE)))
        assertEquals(1, check(add, context(VolumeStatus.IN_RANGE)))
        // Pull-up has back as primary region only here: back above → dropped, chest above alone is not enough.
        val pull = SuggestionChange.AddExercise(7, pullUp.id, 3, 5, 10, null)
        assertEquals(1, check(pull, context(VolumeStatus.ABOVE)))
        assertEquals(0, check(pull, context(VolumeStatus.IN_RANGE, back = VolumeStatus.ABOVE)))
    }

    @Test
    fun otherChangesPass() {
        val ctx = context(VolumeStatus.ABOVE)
        assertEquals(1, check(SuggestionChange.RepRange(7, bench.id, 6, 8, 8, 10), ctx))
        assertEquals(1, check(SuggestionChange.RemoveExercise(7, bench.id), ctx))
    }
}
