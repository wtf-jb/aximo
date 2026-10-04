package io.github.wtfjb.aximo.ui.today

import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Workout
import kotlin.time.Duration.Companion.hours
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.ProgressionReason
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
import kotlinx.datetime.TimeZone
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
        val vm = TodayViewModel(workouts, routines, cardio, exercises, FakeProgressionRepository(), starter, TimeSource { t0 }, TimeZone.UTC)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        val recent = vm.uiState.value.recent

        assertEquals(2, recent.size)
        assertEquals(5L, (recent[0] as RecentItem.WorkoutItem).workoutId)
        assertEquals(1L, (recent[1] as RecentItem.CardioItem).entry.id)
    }

    @Test
    fun heroShowsTheNextRoutineWithProgressionAndTheWeek() = runTest {
        val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, restSeconds = 140)
        val history = mapOf(1L to listOf(SetEntry(workoutExerciseId = 1, position = 0, weightKg = 82.5, reps = 8, completedAt = t0)))
        val workouts = FakeWorkoutRepository(mapOf(1L to bench), history)
        val routine = RoutineWithExercises(
            Routine(id = 3, name = "Push A"),
            listOf(RoutineExercise(routineId = 3, exerciseId = 1, position = 0, targetSets = 3, repMin = 6, repMax = 8)),
        )
        val routines = FakeRoutineRepository(listOf(routine))
        val progression = FakeProgressionRepository(listOf(ProgressionState(1, 85.0, 6, ProgressionReason.INCREASE_WEIGHT)))
        workouts.allFinished.value = listOf(WorkoutDetail(Workout(id = 5, startedAt = t0 - 1.hours, endedAt = t0), emptyList()))
        val starter = WorkoutStarter(workouts, routines, progression, TimeSource { t0 })
        val vm = TodayViewModel(workouts, routines, FakeCardioRepository(emptyList()), FakeExerciseRepository(listOf(bench)), progression, starter, TimeSource { t0 }, TimeZone.UTC)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        val next = vm.uiState.value.next!!
        assertEquals("Push A", next.routine.name)
        assertEquals(1, next.exerciseCount)
        // 3 × (40 + 140) s = 9 min → 10 min
        assertEquals(10, next.estimatedMinutes)
        assertEquals(2.5, next.preview.single().deltaKg!!, 1e-9)
        assertEquals(1, vm.uiState.value.week!!.sessions)
    }
}
