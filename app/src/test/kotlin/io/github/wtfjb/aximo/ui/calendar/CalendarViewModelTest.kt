package io.github.wtfjb.aximo.ui.calendar

import io.github.wtfjb.aximo.domain.calendar.CalendarMonth
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.ui.cardio.FakeCardioRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now = Instant.parse("2026-10-04T12:00:00Z")
    private val run = Exercise(id = 2, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)

    private fun workout(id: Long, start: String) =
        WorkoutDetail(Workout(id = id, startedAt = Instant.parse(start), endedAt = Instant.parse(start) + 50.minutes), emptyList())

    private fun TestScope.viewModel(): CalendarViewModel {
        val workouts = FakeWorkoutRepository(emptyMap())
        workouts.allFinished.value = listOf(workout(1, "2026-09-30T17:00:00Z"), workout(2, "2026-08-12T17:00:00Z"))
        val cardio = FakeCardioRepository(
            listOf(CardioEntry(id = 1, exerciseId = run.id, startedAt = Instant.parse("2026-09-30T06:00:00Z"), durationSec = 1_800)),
        )
        val vm = CalendarViewModel(workouts, FakeRoutineRepository(), cardio, FakeExerciseRepository(listOf(run)), TimeSource { now }, TimeZone.UTC)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        return vm
    }

    @Test
    fun startsOnTodayInThisMonth() = runTest {
        val state = viewModel().uiState.value
        assertEquals(CalendarMonth(2026, 10), state.month)
        assertEquals(LocalDate(2026, 10, 4), state.selected)
        assertTrue(state.selectedItems.isEmpty())
        assertFalse(state.canGoForward)
        assertTrue(state.canGoBack)
    }

    @Test
    fun marksTrainingDaysAndListsTheSelectedDay() = runTest {
        val vm = viewModel()
        vm.previousMonth()
        vm.select(LocalDate(2026, 9, 30))

        val state = vm.uiState.value
        assertEquals(CalendarMonth(2026, 9), state.month)
        assertEquals(DayKind.STRENGTH, state.days[LocalDate(2026, 9, 30)]) // strength wins over cardio
        assertEquals(DayKind.STRENGTH, state.days[LocalDate(2026, 8, 12)])
        assertEquals(2, state.selectedItems.size)
        assertTrue(state.selectedItems[0] is RecentItem.WorkoutItem) // newest first
    }

    @Test
    fun stopsAtTheOldestMonthAndAtTheCurrentOne() = runTest {
        val vm = viewModel()
        vm.previousMonth()
        vm.previousMonth()
        vm.previousMonth()

        assertEquals(CalendarMonth(2026, 8), vm.uiState.value.month)
        assertFalse(vm.uiState.value.canGoBack)
        assertTrue(vm.uiState.value.canGoForward)
    }
}
