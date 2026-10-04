package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.calendar.CalendarMonth
import io.github.wtfjb.aximo.domain.calendar.TrainingCalendar
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.stats.DayKind
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrainingCalendarTest {

    private val zone = TimeZone.UTC
    private val run = Exercise(id = 2, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)

    private fun workout(id: Long, date: String, time: String = "10:00:00") =
        RecentItem.WorkoutItem(id, "Push A", Instant.parse("${date}T${time}Z"), 3_000, 4_000.0)

    private fun cardio(id: Long, date: String) =
        RecentItem.CardioItem(CardioEntry(id = id, exerciseId = run.id, startedAt = Instant.parse("${date}T07:00:00Z"), durationSec = 1_800), run)

    @Test
    fun `weeks start on Monday and pad with null`() {
        // October 2026 starts on a Thursday and ends on a Saturday: 5 weeks.
        val weeks = TrainingCalendar.weeks(CalendarMonth(2026, 10))
        assertEquals(5, weeks.size)
        assertEquals(listOf(null, null, null, LocalDate(2026, 10, 1), LocalDate(2026, 10, 2), LocalDate(2026, 10, 3), LocalDate(2026, 10, 4)), weeks[0])
        assertEquals(listOf(LocalDate(2026, 10, 26), LocalDate(2026, 10, 27), LocalDate(2026, 10, 28), LocalDate(2026, 10, 29), LocalDate(2026, 10, 30), LocalDate(2026, 10, 31), null), weeks[4])
        assertEquals(31, weeks.flatten().filterNotNull().size)
    }

    @Test
    fun `a month starting on Monday and ending on Sunday has exactly four weeks`() {
        // February 2027: Monday 1st to Sunday 28th.
        val weeks = TrainingCalendar.weeks(CalendarMonth(2027, 2))
        assertEquals(4, weeks.size)
        assertEquals(28, weeks.flatten().filterNotNull().size)
    }

    @Test
    fun `a month can span six weeks`() {
        // Aug 2026 starts on a Saturday and has 31 days: Sa, then 4 full weeks, then Mo+Tu... = 6 rows.
        assertEquals(6, TrainingCalendar.weeks(CalendarMonth(2026, 8)).size)
    }

    @Test
    fun `months move across the year boundary`() {
        assertEquals(CalendarMonth(2026, 12), CalendarMonth(2027, 1).plusMonths(-1))
        assertEquals(CalendarMonth(2027, 1), CalendarMonth(2026, 12).plusMonths(1))
    }

    @Test
    fun `items of a day are filtered in the local time zone and sorted newest first`() {
        val items = listOf(
            workout(1, "2026-10-02", "08:00:00"),
            workout(2, "2026-10-02", "18:00:00"),
            cardio(3, "2026-10-03"),
        )
        val day = TrainingCalendar.itemsOn(LocalDate(2026, 10, 2), items, zone)
        assertEquals(listOf(2L, 1L), day.map { (it as RecentItem.WorkoutItem).workoutId })
        assertEquals(1, TrainingCalendar.itemsOn(LocalDate(2026, 10, 3), items, zone).size)
        assertEquals(0, TrainingCalendar.itemsOn(LocalDate(2026, 10, 4), items, zone).size)
        // 23:30 UTC is already the next day in Berlin (UTC+2 in October).
        val late = listOf(workout(4, "2026-10-02", "23:30:00"))
        assertEquals(1, TrainingCalendar.itemsOn(LocalDate(2026, 10, 3), late, TimeZone.of("Europe/Berlin")).size)
    }

    @Test
    fun `kind defaults to none`() {
        val days = mapOf(LocalDate(2026, 10, 2) to DayKind.STRENGTH)
        assertEquals(DayKind.STRENGTH, TrainingCalendar.kind(LocalDate(2026, 10, 2), days))
        assertEquals(DayKind.NONE, TrainingCalendar.kind(LocalDate(2026, 10, 3), days))
    }

    @Test
    fun `calendar reaches back to the oldest item and forward to today`() {
        val today = LocalDate(2026, 10, 4)
        val items = listOf(workout(1, "2026-07-15"), cardio(2, "2026-09-01"))
        val first = TrainingCalendar.firstMonth(items, today, zone)
        assertEquals(CalendarMonth(2026, 7), first)
        assertNull(TrainingCalendar.previous(CalendarMonth(2026, 7), first))
        assertEquals(CalendarMonth(2026, 7), TrainingCalendar.previous(CalendarMonth(2026, 8), first))
        assertEquals(CalendarMonth(2026, 10), TrainingCalendar.next(CalendarMonth(2026, 9), today))
        assertNull(TrainingCalendar.next(CalendarMonth(2026, 10), today))
        assertEquals(CalendarMonth(2026, 10), TrainingCalendar.firstMonth(emptyList(), today, zone))
    }
}
