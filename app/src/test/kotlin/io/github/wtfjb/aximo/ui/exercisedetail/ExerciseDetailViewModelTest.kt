package io.github.wtfjb.aximo.ui.exercisedetail

import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.stats.StatsTestWorkouts.bench
import io.github.wtfjb.aximo.ui.stats.StatsTestWorkouts.workout
import io.github.wtfjb.aximo.ui.workout.FakeProgressionRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val workouts = FakeWorkoutRepository(mapOf(1L to bench))
    private val progression = FakeProgressionRepository(listOf(ProgressionState(1, 87.5, 6, ProgressionReason.INCREASE_WEIGHT)))
    private val vm by lazy {
        ExerciseDetailViewModel(FakeExerciseRepository(listOf(bench)), workouts, FakeRoutineRepository(), progression, exerciseId = 1)
    }

    @Test
    fun showsHistoryNewestFirstWithRecordsAndSuggestion() = runTest(UnconfinedTestDispatcher()) {
        workouts.allFinished.value = listOf(workout(1, 10, 80.0, 8), workout(2, 3, 85.0, 6))
        vm.uiState.launchIn(backgroundScope)

        val state = vm.uiState.value
        assertEquals(bench, state.exercise)
        assertEquals(listOf(2L, 1L), state.sessions.map { it.workoutId })
        assertEquals(setOf(2L), state.recordWorkouts)
        assertEquals(85.0, state.bests!!.maxWeightKg!!, 0.0)
        assertEquals(2, state.bests!!.sessions)
        assertEquals(87.5, state.suggestion!!.nextWeightKg, 0.0)
        assertEquals(1, state.lastWorkingSets)
    }

    @Test
    fun tabsSwitch() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)
        vm.onTabSelect(DetailTab.INFO)
        assertEquals(DetailTab.INFO, vm.uiState.value.tab)
    }
}
