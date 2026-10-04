package io.github.wtfjb.aximo.domain.chat

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.pullUp
import io.github.wtfjb.aximo.domain.StatsTestData.set
import io.github.wtfjb.aximo.domain.StatsTestData.workout
import io.github.wtfjb.aximo.domain.StatsTestData.zone
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContextBuilderTest {

    private val today = LocalDate(2026, 10, 4)

    @Test
    fun containsTheReviewContext() {
        val workouts = listOf(workout(1, at(LocalDate(2026, 10, 1)), bench to listOf(set(80.0, 8))))

        val context = ChatContextBuilder.build(workouts, emptyList(), listOf(bench), weeklyGoal = 3, today = today, zone = zone)

        assertEquals(1, context.review.sessions)
        assertEquals(3, context.review.weeklyGoal)
        assertEquals("Bankdrücken", context.history.single().name)
    }

    @Test
    fun bestValuePerBlockOfFourWeeks() {
        // 24 weeks up to today: 20 Apr – 4 Oct, blocks start 20 Apr, 18 May, 15 Jun, 13 Jul, 10 Aug, 7 Sep.
        val workouts = listOf(
            workout(1, at(LocalDate(2026, 4, 19)), bench to listOf(set(120.0, 8))), // before the period
            workout(2, at(LocalDate(2026, 4, 20)), bench to listOf(set(80.0, 8))),
            workout(3, at(LocalDate(2026, 6, 1)), pullUp to listOf(set(0.0, 10), set(0.0, 8))),
            workout(4, at(LocalDate(2026, 9, 7)), bench to listOf(set(82.5, 8))),
            workout(5, at(LocalDate(2026, 10, 1)), bench to listOf(set(85.0, 8), set(40.0, 10))),
            workout(6, at(LocalDate(2026, 10, 3)), bench to listOf(set(200.0, 1)), finished = false), // running
        )

        val history = ChatContextBuilder.history(workouts, today, zone)

        val benchHistory = history.first()
        assertEquals(bench.id, benchHistory.exerciseId)
        assertEquals(ProgressMetric.E1RM, benchHistory.metric)
        assertEquals(ChatContextBuilder.BLOCKS, benchHistory.blocks.size)
        assertEquals(LocalDate(2026, 4, 20), benchHistory.blocks.first().start)
        assertEquals(LocalDate(2026, 9, 7), benchHistory.blocks.last().start)
        assertEquals(101.3, benchHistory.blocks.first().best!!, 0.0) // 80 × (1 + 8/30)
        assertEquals(1, benchHistory.blocks.first().sessions)
        assertNull(benchHistory.blocks[1].best)
        assertEquals(0, benchHistory.blocks[1].sessions)
        assertEquals(2, benchHistory.blocks.last().sessions)
        assertEquals(107.7, benchHistory.blocks.last().best!!, 0.0)

        val pullUpHistory = history[1]
        assertEquals(ProgressMetric.REPS, pullUpHistory.metric)
        assertEquals(10.0, pullUpHistory.blocks[1].best!!, 0.0)
    }

    @Test
    fun leavesOutCardioAndKeepsTheMostTrained() {
        val run = bench.copy(id = 50, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)
        val others = (1..ChatContextBuilder.MAX_HISTORY_EXERCISES + 2).map { bench.copy(id = 100L + it, name = "Übung $it") }
        val workouts = others.mapIndexed { index, e ->
            workout(index + 1L, at(LocalDate(2026, 9, 1)), e to listOf(set(50.0, 5)), run to listOf(set(0.0, 1)))
        } + workout(99, at(LocalDate(2026, 9, 2)), others.last() to listOf(set(50.0, 5)))

        val history = ChatContextBuilder.history(workouts, today, zone)

        assertEquals(ChatContextBuilder.MAX_HISTORY_EXERCISES, history.size)
        assertEquals(others.last().id, history.first().exerciseId)
        assertTrue(history.none { it.exerciseId == run.id })
    }
}
