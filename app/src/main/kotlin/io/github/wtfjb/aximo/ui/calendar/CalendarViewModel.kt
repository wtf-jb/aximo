package io.github.wtfjb.aximo.ui.calendar

import androidx.lifecycle.ViewModel
import io.github.wtfjb.aximo.domain.calendar.CalendarMonth
import io.github.wtfjb.aximo.domain.calendar.TrainingCalendar
import io.github.wtfjb.aximo.domain.recent.RecentActivity
import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.repository.CardioRepository
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.stats.Consistency
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.domain.time.TimeSource
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

data class CalendarUiState(
    val today: LocalDate,
    val month: CalendarMonth,
    /** All months from the oldest training to today, oldest first: one pager page each. */
    val months: List<CalendarMonth> = emptyList(),
    val days: Map<LocalDate, DayKind> = emptyMap(),
    val selected: LocalDate,
    /** Workouts and cardio of the selected day. */
    val selectedItems: List<RecentItem> = emptyList(),
)

/**
 * Calendar of training days: strength and cardio days are marked, a tap on a day
 * lists what was done and opens it.
 */
class CalendarViewModel(
    workouts: WorkoutRepository,
    routines: RoutineRepository,
    cardio: CardioRepository,
    exercises: ExerciseRepository,
    time: TimeSource,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val today = StatsCalendar.localDate(time.now(), zone)

    private data class View(val month: CalendarMonth, val selected: LocalDate)

    private val view = MutableStateFlow(View(CalendarMonth.of(today), today))

    private val items = combine(
        workouts.observeFinished(),
        routines.observeRoutines(),
        cardio.observeAll(),
        exercises.observeExercises(includeArchived = true),
    ) { finished, list, entries, all ->
        RecentActivity.merge(finished, list, entries, all, limit = Int.MAX_VALUE)
    }

    val uiState: StateFlow<CalendarUiState> = combine(items, view) { all, (month, selected) ->
        val days = Consistency.activeDays(
            strength = all.filterIsInstance<RecentItem.WorkoutItem>().map { it.startedAt },
            cardio = all.filterIsInstance<RecentItem.CardioItem>().map { it.startedAt },
            zone = zone,
        )
        val first = TrainingCalendar.firstMonth(all, today, zone)
        CalendarUiState(
            today = today,
            month = month,
            months = TrainingCalendar.months(first, today),
            days = days,
            selected = selected,
            selectedItems = TrainingCalendar.itemsOn(selected, all, zone),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(today = today, month = CalendarMonth.of(today), months = listOf(CalendarMonth.of(today)), selected = today),
    )

    /** Called when the pager settles on a month; months outside the range are ignored. */
    fun showMonth(month: CalendarMonth) {
        if (month in uiState.value.months) view.update { it.copy(month = month) }
    }

    fun select(date: LocalDate) {
        view.update { it.copy(selected = date) }
    }
}
