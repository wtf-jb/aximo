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
    /** Weeks of the month, Monday to Sunday; days outside the month are null. */
    val weeks: List<List<LocalDate?>> = emptyList(),
    val days: Map<LocalDate, DayKind> = emptyMap(),
    val selected: LocalDate,
    /** Workouts and cardio of the selected day. */
    val selectedItems: List<RecentItem> = emptyList(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
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

    /** Latest items, kept for the month limits. */
    private val items = MutableStateFlow<List<RecentItem>>(emptyList())

    private val view = MutableStateFlow(View(CalendarMonth.of(today), today))

    private val itemFlow = combine(
        workouts.observeFinished(),
        routines.observeRoutines(),
        cardio.observeAll(),
        exercises.observeExercises(includeArchived = true),
    ) { finished, list, entries, all ->
        RecentActivity.merge(finished, list, entries, all, limit = Int.MAX_VALUE)
    }

    val uiState: StateFlow<CalendarUiState> = combine(itemFlow, view) { all, (month, selected) ->
        val days = Consistency.activeDays(
            strength = all.filterIsInstance<RecentItem.WorkoutItem>().map { it.startedAt },
            cardio = all.filterIsInstance<RecentItem.CardioItem>().map { it.startedAt },
            zone = zone,
        )
        items.value = all
        val first = TrainingCalendar.firstMonth(all, today, zone)
        CalendarUiState(
            today = today,
            month = month,
            weeks = TrainingCalendar.weeks(month),
            days = days,
            selected = selected,
            selectedItems = TrainingCalendar.itemsOn(selected, all, zone),
            canGoBack = TrainingCalendar.previous(month, first) != null,
            canGoForward = TrainingCalendar.next(month, today) != null,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(today = today, month = CalendarMonth.of(today), selected = today),
    )

    fun previousMonth() {
        val first = TrainingCalendar.firstMonth(items.value, today, zone)
        view.update { current ->
            TrainingCalendar.previous(current.month, first)?.let { current.copy(month = it) } ?: current
        }
    }

    fun nextMonth() {
        view.update { current ->
            TrainingCalendar.next(current.month, today)?.let { current.copy(month = it) } ?: current
        }
    }

    fun select(date: LocalDate) {
        view.update { it.copy(selected = date) }
    }
}
