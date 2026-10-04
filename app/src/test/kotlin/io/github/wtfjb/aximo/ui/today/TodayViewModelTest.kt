package io.github.wtfjb.aximo.ui.today

import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import io.github.wtfjb.aximo.ui.cardio.FakeCardioRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.workout.FakeProgressionRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TodayViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val t0 = Instant.fromEpochSeconds(1_790_000_000)
    private val run = Exercise(id = 2, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)

    @Test
    fun recentMergesWorkoutsAndCardio() = runTest {
        val workouts = FakeWorkoutRepository(emptyMap())
        val routines = FakeRoutineRepository()
        val exercises = FakeExerciseRepository(listOf(run))
        val cardio = FakeCardioRepository(listOf(CardioEntry(id = 1, exerciseId = run.id, startedAt = t0 - 1.days, durationSec = 1721)))
        workouts.recentFinished.value = listOf(WorkoutDetail(Workout(id = 5, startedAt = t0, endedAt = t0 + 50.minutes), emptyList()))
        val starter = WorkoutStarter(workouts, routines, FakeProgressionRepository(), TimeSource { t0 })
        val vm = TodayViewModel(workouts, routines, cardio, exercises, starter)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        val recent = vm.uiState.value.recent

        assertEquals(2, recent.size)
        assertEquals(5L, (recent[0] as RecentItem.WorkoutItem).workoutId)
        assertEquals(1L, (recent[1] as RecentItem.CardioItem).entry.id)
    }
}
