package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.zone
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.domain.stats.StatsPeriod
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsPeriodTest {

    private val wednesday = LocalDate(2026, 10, 7)

    @Test
    fun `week starts on monday`() {
        assertEquals(LocalDate(2026, 10, 5), StatsCalendar.weekStart(wednesday))
        assertEquals(LocalDate(2026, 10, 5), StatsCalendar.weekStart(LocalDate(2026, 10, 5)))
        assertEquals(LocalDate(2026, 10, 5), StatsCalendar.weekStart(LocalDate(2026, 10, 11)))
    }

    @Test
    fun `period starts on the monday of the oldest week`() {
        assertEquals(LocalDate(2026, 9, 14), StatsCalendar.periodStart(StatsPeriod.FOUR_WEEKS, wednesday))
        assertEquals(LocalDate(2026, 7, 13), StatsCalendar.periodStart(StatsPeriod.THREE_MONTHS, wednesday))
        assertNull(StatsCalendar.periodStart(StatsPeriod.ALL, wednesday))
    }

    @Test
    fun `in period compares local dates`() {
        assertTrue(StatsCalendar.inPeriod(at(LocalDate(2026, 9, 14)), StatsPeriod.FOUR_WEEKS, wednesday, zone))
        assertFalse(StatsCalendar.inPeriod(at(LocalDate(2026, 9, 13)), StatsPeriod.FOUR_WEEKS, wednesday, zone))
        assertTrue(StatsCalendar.inPeriod(at(LocalDate(2020, 1, 1)), StatsPeriod.ALL, wednesday, zone))
    }

    @Test
    fun `week count for all time runs from the first week`() {
        assertEquals(4, StatsCalendar.weekCount(StatsPeriod.FOUR_WEEKS, wednesday, null))
        assertEquals(1, StatsCalendar.weekCount(StatsPeriod.ALL, wednesday, null))
        assertEquals(1, StatsCalendar.weekCount(StatsPeriod.ALL, wednesday, LocalDate(2026, 10, 5)))
        assertEquals(3, StatsCalendar.weekCount(StatsPeriod.ALL, wednesday, LocalDate(2026, 9, 27)))
    }

    @Test
    fun relativeChange() {
        assertEquals(0.1, StatsCalendar.relativeChange(100.0, 110.0)!!, 1e-9)
        assertEquals(-0.5, StatsCalendar.relativeChange(100.0, 50.0)!!, 1e-9)
        assertNull(StatsCalendar.relativeChange(0.0, 50.0))
    }
}
