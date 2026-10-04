package io.github.wtfjb.aximo.domain.review

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** When the weekly review runs by itself (B-02). Off by default. */
data class WeeklyReviewSetting(
    val enabled: Boolean = false,
    val day: DayOfWeek = DayOfWeek.SUNDAY,
    val hour: Int = DEFAULT_HOUR,
) {
    init {
        require(hour in 0..23) { "hour must be 0–23" }
    }

    companion object {
        /** Mockup: "Sonntag, 18:00". */
        const val DEFAULT_HOUR = 18
    }
}

object WeeklySchedule {
    /** The next [day] at [hour]:00 in [zone] strictly after [now]. */
    fun nextRun(now: Instant, day: DayOfWeek, hour: Int, zone: TimeZone): Instant {
        val today = now.toLocalDateTime(zone).date
        val daysAhead = (day.ordinal - today.dayOfWeek.ordinal + DAYS) % DAYS
        var candidate = LocalDateTime(today.plus(daysAhead, DateTimeUnit.DAY), LocalTime(hour, 0)).toInstant(zone)
        if (candidate <= now) {
            candidate = LocalDateTime(today.plus(daysAhead + DAYS, DateTimeUnit.DAY), LocalTime(hour, 0)).toInstant(zone)
        }
        return candidate
    }

    private const val DAYS = 7
}
