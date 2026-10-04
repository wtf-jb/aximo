package io.github.wtfjb.aximo.domain.stats

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Time filter of the statistics (A-07). Periods are whole calendar weeks
 * (Monday to Sunday) up to the current week, so averages per week are fair.
 * [weeks] is null for "all time".
 */
enum class StatsPeriod(val weeks: Int?) {
    FOUR_WEEKS(4),
    THREE_MONTHS(13),
    ONE_YEAR(52),
    ALL(null),
}

/** Calendar helpers for the statistics. Weeks start on Monday (ISO). */
object StatsCalendar {

    fun localDate(instant: Instant, zone: TimeZone): LocalDate = instant.toLocalDateTime(zone).date

    /** Monday of the week of [date]. */
    fun weekStart(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    /** First day of the period (a Monday), or null for [StatsPeriod.ALL]. */
    fun periodStart(period: StatsPeriod, today: LocalDate): LocalDate? =
        period.weeks?.let { weekStart(today).minus((it - 1) * DAYS_PER_WEEK, DateTimeUnit.DAY) }

    /** True if [instant] lies in the period ending today. */
    fun inPeriod(instant: Instant, period: StatsPeriod, today: LocalDate, zone: TimeZone): Boolean {
        val start = periodStart(period, today) ?: return true
        return localDate(instant, zone) >= start
    }

    /**
     * Number of weeks for "per week" averages: the length of the period; for
     * [StatsPeriod.ALL] the weeks from the week of [first] to the current one.
     * Always at least 1.
     */
    fun weekCount(period: StatsPeriod, today: LocalDate, first: LocalDate?): Int {
        period.weeks?.let { return it }
        if (first == null) return 1
        val days = weekStart(first).daysUntil(weekStart(today))
        return (days / DAYS_PER_WEEK + 1).coerceAtLeast(1)
    }

    /** Relative change from [from] to [to], e.g. 0.091 = +9.1 %. Null if [from] is 0. */
    fun relativeChange(from: Double, to: Double): Double? = if (from == 0.0) null else (to - from) / from

    const val DAYS_PER_WEEK = 7
}
