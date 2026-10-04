package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.pullUp
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.today.NextWorkout
import io.github.wtfjb.aximo.domain.today.WeekBar
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextWorkoutTest {

    private val exercises = mapOf(bench.id to bench.copy(restSeconds = 180), pullUp.id to pullUp.copy(restSeconds = 120))

    private fun target(exerciseId: Long, sets: Int, position: Int = 0) =
        RoutineExercise(exerciseId = exerciseId, position = position, targetSets = sets, repMin = 6, repMax = 8)

    @Test
    fun estimateAddsSetTimeAndRestRoundedUpToFiveMinutes() {
        // 3 × (40 + 180) + 3 × (40 + 120) = 1140 s = 19 min → 20 min
        assertEquals(20, NextWorkout.estimatedMinutes(listOf(target(1, 3), target(2, 3, 1)), exercises))
        assertEquals(0, NextWorkout.estimatedMinutes(emptyList(), exercises))
    }

    @Test
    fun unknownExercisesUseTheDefaultRest() {
        // 2 × (40 + 120) = 320 s → 10 min
        assertEquals(10, NextWorkout.estimatedMinutes(listOf(target(99, 2)), exercises))
    }

    @Test
    fun previewShowsChangesInRoutineOrderWithDelta() {
        val suggestions = mapOf(
            1L to ProgressionState(1, 85.0, 6, ProgressionReason.INCREASE_WEIGHT),
            2L to ProgressionState(2, 0.0, 9, ProgressionReason.INCREASE_REPS),
        )
        val preview = NextWorkout.preview(listOf(target(2, 3), target(1, 3, 1)), exercises, suggestions, mapOf(1L to 82.5))

        assertEquals(listOf(2L, 1L), preview.map { it.exerciseId })
        assertEquals(2.5, preview[1].deltaKg!!, 1e-9)
        assertNull(preview[0].deltaKg)
        assertEquals(9, preview[0].nextRepTarget)
    }

    @Test
    fun holdAndMissingSuggestionsAreLeftOut() {
        val suggestions = mapOf(1L to ProgressionState(1, 80.0, 7, ProgressionReason.HOLD))
        assertTrue(NextWorkout.preview(listOf(target(1, 3), target(2, 3, 1)), exercises, suggestions, emptyMap()).isEmpty())
    }

    @Test
    fun previewIsLimited() {
        val suggestions = mapOf(1L to ProgressionState(1, 85.0, 6, ProgressionReason.INCREASE_WEIGHT), 2L to ProgressionState(2, 5.0, 6, ProgressionReason.INCREASE_WEIGHT))
        assertEquals(1, NextWorkout.preview(listOf(target(1, 3), target(2, 3, 1)), exercises, suggestions, emptyMap(), limit = 1).size)
    }

    @Test
    fun weekBarRunsMondayToSundayAndCountsSessions() {
        // Saturday, 3 October 2026.
        val today = LocalDate(2026, 10, 3)
        val strength = listOf(at(LocalDate(2026, 9, 28)), at(LocalDate(2026, 10, 1)), at(LocalDate(2026, 9, 27)))
        val cardio = listOf(at(LocalDate(2026, 9, 29)), at(LocalDate(2026, 10, 1)))

        val week = WeekBar.overview(strength, cardio, today, TimeZone.UTC)

        assertEquals(LocalDate(2026, 9, 28), week.days.first().date)
        assertEquals(7, week.days.size)
        assertEquals(
            listOf(DayKind.STRENGTH, DayKind.CARDIO, DayKind.NONE, DayKind.STRENGTH, DayKind.NONE, DayKind.NONE, DayKind.FUTURE),
            week.days.map { it.kind },
        )
        assertEquals(listOf(5), week.days.withIndex().filter { it.value.isToday }.map { it.index })
        // Sunday before the week doesn't count; the strength and cardio session on Thursday count twice.
        assertEquals(4, week.sessions)
    }
}
