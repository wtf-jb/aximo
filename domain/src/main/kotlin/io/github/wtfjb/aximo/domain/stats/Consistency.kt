package io.github.wtfjb.aximo.domain.stats

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.minus

/** What happened on one day of the heatmap. Strength wins if a day has both. */
enum class DayKind { NONE, STRENGTH, CARDIO, FUTURE }

/** Training frequency: heatmap, sessions per week, streak (A-07). */
object Consistency {
    const val HEATMAP_WEEKS = 12

    /** Training days from workout and cardio start times. */
    fun activeDays(strength: List<Instant>, cardio: List<Instant>, zone: TimeZone): Map<LocalDate, DayKind> {
        val days = mutableMapOf<LocalDate, DayKind>()
        cardio.forEach { days[StatsCalendar.localDate(it, zone)] = DayKind.CARDIO }
        strength.forEach { days[StatsCalendar.localDate(it, zone)] = DayKind.STRENGTH }
        return days
    }

    /**
     * The last [weeks] weeks up to the current one, oldest first. Each week has
     * seven days from Monday to Sunday; days after [today] are [DayKind.FUTURE].
     */
    fun heatmap(days: Map<LocalDate, DayKind>, today: LocalDate, weeks: Int = HEATMAP_WEEKS): List<List<DayKind>> {
        val first = StatsCalendar.weekStart(today).minus((weeks - 1) * StatsCalendar.DAYS_PER_WEEK, DateTimeUnit.DAY)
        return List(weeks) { week ->
            List(StatsCalendar.DAYS_PER_WEEK) { day ->
                val date = first.plus(week * StatsCalendar.DAYS_PER_WEEK + day, DateTimeUnit.DAY)
                if (date > today) DayKind.FUTURE else days[date] ?: DayKind.NONE
            }
        }
    }

    fun perWeek(sessions: Int, weeks: Int): Double = sessions.toDouble() / weeks.coerceAtLeast(1)

    /**
     * Weeks in a row with at least one session, counted back from the current
     * week. A current week without a session doesn't break the streak yet.
     */
    fun streakWeeks(days: Set<LocalDate>, today: LocalDate): Int {
        val activeWeeks = days.map { StatsCalendar.weekStart(it) }.toSet()
        var week = StatsCalendar.weekStart(today)
        if (week !in activeWeeks) week = week.minus(StatsCalendar.DAYS_PER_WEEK, DateTimeUnit.DAY)
        var streak = 0
        while (week in activeWeeks) {
            streak++
            week = week.minus(StatsCalendar.DAYS_PER_WEEK, DateTimeUnit.DAY)
        }
        return streak
    }
}
