package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.zone
import io.github.wtfjb.aximo.domain.stats.Consistency
import io.github.wtfjb.aximo.domain.stats.DayKind
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsistencyTest {

    private val wednesday = LocalDate(2026, 10, 7)

    @Test
    fun `strength wins over cardio on the same day`() {
        val days = Consistency.activeDays(
            strength = listOf(at(LocalDate(2026, 10, 5))),
            cardio = listOf(at(LocalDate(2026, 10, 5)), at(LocalDate(2026, 10, 6))),
            zone = zone,
        )
        assertEquals(DayKind.STRENGTH, days[LocalDate(2026, 10, 5)])
        assertEquals(DayKind.CARDIO, days[LocalDate(2026, 10, 6)])
    }

    @Test
    fun `heatmap has weeks from monday to sunday, future days marked`() {
        val days = mapOf(
            LocalDate(2026, 10, 5) to DayKind.STRENGTH,
            LocalDate(2026, 7, 20) to DayKind.CARDIO,
            LocalDate(2026, 7, 19) to DayKind.STRENGTH, // before the first week
        )
        val map = Consistency.heatmap(days, wednesday)
        assertEquals(12, map.size)
        map.forEach { assertEquals(7, it.size) }
        assertEquals(DayKind.CARDIO, map.first()[0])
        val current = map.last()
        assertEquals(listOf(DayKind.STRENGTH, DayKind.NONE, DayKind.NONE), current.take(3))
        assertEquals(List(4) { DayKind.FUTURE }, current.drop(3))
        assertEquals(2, map.flatten().count { it == DayKind.STRENGTH || it == DayKind.CARDIO })
    }

    @Test
    fun perWeek() {
        assertEquals(3.25, Consistency.perWeek(13, 4), 0.0)
        assertEquals(5.0, Consistency.perWeek(5, 0), 0.0)
    }

    @Test
    fun `streak counts weeks back from now`() {
        val days = setOf(
            LocalDate(2026, 10, 6), // this week
            LocalDate(2026, 9, 28),
            LocalDate(2026, 9, 21),
            LocalDate(2026, 9, 7), // gap in the week of 14 Sep
        )
        assertEquals(3, Consistency.streakWeeks(days, wednesday))
    }

    @Test
    fun `empty current week does not break the streak yet`() {
        val days = setOf(LocalDate(2026, 10, 1), LocalDate(2026, 9, 22))
        assertEquals(2, Consistency.streakWeeks(days, wednesday))
        assertEquals(0, Consistency.streakWeeks(setOf(LocalDate(2026, 9, 22)), wednesday))
        assertEquals(0, Consistency.streakWeeks(emptySet(), wednesday))
    }
}
