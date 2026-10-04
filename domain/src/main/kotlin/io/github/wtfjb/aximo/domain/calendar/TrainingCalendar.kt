package io.github.wtfjb.aximo.domain.calendar

import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** A month shown in the calendar. */
data class CalendarMonth(val year: Int, val month: Int) {
    val first: LocalDate get() = LocalDate(year, month, 1)

    fun plusMonths(count: Int): CalendarMonth {
        val moved = first.plus(count, DateTimeUnit.MONTH)
        return CalendarMonth(moved.year, moved.month.ordinal + 1)
    }

    companion object {
        fun of(date: LocalDate) = CalendarMonth(date.year, date.month.ordinal + 1)
    }
}

/** Month view of the training days: grid layout and the items of a day. */
object TrainingCalendar {

    /**
     * The weeks of [month], Monday to Sunday. Days outside the month are null,
     * so every week has seven cells.
     */
    fun weeks(month: CalendarMonth): List<List<LocalDate?>> {
        val first = month.first
        val next = first.plus(1, DateTimeUnit.MONTH)
        val start = StatsCalendar.weekStart(first)
        val weekCount = (start.daysUntil(next) + StatsCalendar.DAYS_PER_WEEK - 1) / StatsCalendar.DAYS_PER_WEEK
        return List(weekCount) { week ->
            List(StatsCalendar.DAYS_PER_WEEK) { day ->
                val date = start.plus(week * StatsCalendar.DAYS_PER_WEEK + day, DateTimeUnit.DAY)
                if (date >= first && date < next) date else null
            }
        }
    }

    /** What happened on [date]; strength wins over cardio. */
    fun kind(date: LocalDate, days: Map<LocalDate, DayKind>): DayKind = days[date] ?: DayKind.NONE

    /** The items that started on [date], newest first. */
    fun itemsOn(date: LocalDate, items: List<RecentItem>, zone: TimeZone): List<RecentItem> =
        items.filter { StatsCalendar.localDate(it.startedAt, zone) == date }.sortedByDescending { it.startedAt }

    /** The month of the oldest item, or [today]'s month without items: how far back the calendar goes. */
    fun firstMonth(items: List<RecentItem>, today: LocalDate, zone: TimeZone): CalendarMonth {
        val oldest = items.minOfOrNull { it.startedAt } ?: return CalendarMonth.of(today)
        return CalendarMonth.of(minOf(StatsCalendar.localDate(oldest, zone), today))
    }

    /** All months from [first] to the month of [today], oldest first: the pages of the calendar. */
    fun months(first: CalendarMonth, today: LocalDate): List<CalendarMonth> {
        val last = CalendarMonth.of(today)
        val result = mutableListOf<CalendarMonth>()
        var month = first
        while (month.first <= last.first) {
            result += month
            month = month.plusMonths(1)
        }
        return result.ifEmpty { listOf(last) }
    }
}
