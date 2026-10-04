package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.review.WeeklyReviewSetting
import io.github.wtfjb.aximo.domain.review.WeeklySchedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class WeeklyScheduleTest {

    private val berlin = TimeZone.of("Europe/Berlin")

    // Sunday, 4 Oct 2026, 12:00 in Berlin (UTC+2).
    private val sundayNoon = Instant.parse("2026-10-04T10:00:00Z")

    @Test
    fun laterTodayIfNotPassed() {
        assertEquals(Instant.parse("2026-10-04T16:00:00Z"), WeeklySchedule.nextRun(sundayNoon, DayOfWeek.SUNDAY, 18, berlin))
    }

    @Test
    fun nextWeekIfPassedOrExactlyNow() {
        assertEquals(Instant.parse("2026-10-11T07:00:00Z"), WeeklySchedule.nextRun(sundayNoon, DayOfWeek.SUNDAY, 9, berlin))
        assertEquals(Instant.parse("2026-10-11T10:00:00Z"), WeeklySchedule.nextRun(sundayNoon, DayOfWeek.SUNDAY, 12, berlin))
    }

    @Test
    fun otherDaysAndDst() {
        assertEquals(Instant.parse("2026-10-05T16:00:00Z"), WeeklySchedule.nextRun(sundayNoon, DayOfWeek.MONDAY, 18, berlin))
        assertEquals(Instant.parse("2026-10-10T16:00:00Z"), WeeklySchedule.nextRun(sundayNoon, DayOfWeek.SATURDAY, 18, berlin))
        // Clocks go back on 25 Oct: 18:00 is then UTC+1.
        val afterSwitch = Instant.parse("2026-10-24T20:00:00Z")
        assertEquals(Instant.parse("2026-10-25T17:00:00Z"), WeeklySchedule.nextRun(afterSwitch, DayOfWeek.SUNDAY, 18, berlin))
    }

    @Test
    fun defaults() {
        val setting = WeeklyReviewSetting()
        assertEquals(false, setting.enabled)
        assertEquals(DayOfWeek.SUNDAY, setting.day)
        assertEquals(18, setting.hour)
    }
}
