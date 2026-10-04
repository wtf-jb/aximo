package io.github.wtfjb.aximo.ui.stats

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.stats.StatsPeriod
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.ui.cardio.FakeCardioRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.stats.StatsTestWorkouts.bench
import io.github.wtfjb.aximo.ui.stats.StatsTestWorkouts.now
import io.github.wtfjb.aximo.ui.stats.StatsTestWorkouts.workout
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val workouts = FakeWorkoutRepository(mapOf(1L to bench))
    private val vm by lazy {
        StatsViewModel(workouts, FakeCardioRepository(), FakeExerciseRepository(listOf(bench)), TimeSource { now }, TimeZone.UTC)
    }

    @Test
    fun emptyWithoutData() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun progressFollowsThePeriod() = runTest(UnconfinedTestDispatcher()) {
        workouts.allFinished.value = listOf(workout(1, 60, 80.0, 8), workout(2, 10, 82.5, 8), workout(3, 3, 85.0, 6))
        vm.uiState.launchIn(backgroundScope)

        val threeMonths = vm.uiState.value.progress!!
        assertEquals(bench, threeMonths.exercise)
        assertEquals(3, threeMonths.points.size)

        vm.onPeriodChange(StatsPeriod.FOUR_WEEKS)
        assertEquals(2, vm.uiState.value.progress!!.points.size)
    }

    @Test
    fun volumeAndConsistencyAreFilled() = runTest(UnconfinedTestDispatcher()) {
        workouts.allFinished.value = listOf(workout(1, 3, 80.0, 8), workout(2, 1, 80.0, 8))
        vm.uiState.launchIn(backgroundScope)

        val state = vm.uiState.value
        assertTrue(state.regions.any { it.region == BodyRegion.CHEST && it.setsPerWeek > 0 })
        assertEquals(2, state.consistency!!.sessions)
    }
}
