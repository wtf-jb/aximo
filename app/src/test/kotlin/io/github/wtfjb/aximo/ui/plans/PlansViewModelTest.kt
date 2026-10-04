package io.github.wtfjb.aximo.ui.plans

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlansViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now = Instant.fromEpochSeconds(1_790_000_000)
    private val bench = Exercise(
        id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL,
        primaryMuscles = setOf(MuscleGroup.CHEST), repRangeMin = 6, repRangeMax = 8,
    )
    private val row = Exercise(
        id = 2, name = "Rudern", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL,
        primaryMuscles = setOf(MuscleGroup.UPPER_BACK),
    )
    private fun target(exerciseId: Long, position: Int, group: String? = null) =
        RoutineExercise(exerciseId = exerciseId, position = position, targetSets = 2, repMin = 6, repMax = 8, targetRir = 2, supersetGroup = group)

    private val push = RoutineWithExercises(Routine(id = 1, name = "Push", position = 0), listOf(target(1, 0)))
    private val pull = RoutineWithExercises(Routine(id = 2, name = "Pull", position = 1), listOf(target(2, 0)))

    private val routines = FakeRoutineRepository(listOf(push, pull))
    private val history = mapOf(1L to listOf(SetEntry(workoutExerciseId = 9, position = 0, weightKg = 80.0, reps = 8, completedAt = now)))
    private val workouts = FakeWorkoutRepository(mapOf(1L to bench, 2L to row), history)
    private val starter = WorkoutStarter(workouts, routines, io.github.wtfjb.aximo.ui.workout.FakeProgressionRepository(), TimeSource { now })
    private val vm by lazy { PlansViewModel(routines, FakeExerciseRepository(listOf(bench, row)), workouts, starter) }

    @Test
    fun listShowsCountsRegionsAndNext() = runTest(UnconfinedTestDispatcher()) {
        workouts.lastPerRoutine.value = mapOf(1L to now)
        vm.uiState.launchIn(backgroundScope)

        val items = vm.uiState.value.routines
        assertEquals(listOf("Push", "Pull"), items.map { it.routine.name })
        assertEquals(listOf(BodyRegion.CHEST), items[0].regions)
        assertEquals(now, items[0].lastTrained)
        assertEquals(listOf(false, true), items.map { it.isNext })
    }

    @Test
    fun startingARoutineCreatesTheWorkoutFromItsTargets() = runTest(UnconfinedTestDispatcher()) {
        var opened = false
        vm.start(1L) { opened = true }

        assertTrue(opened)
        val workout = workouts.current!!
        assertEquals(1L, workout.workout.routineId)
        val sets = workout.exercises.single().sets
        assertEquals(listOf(80.0, 80.0), sets.map { it.weightKg })
        assertEquals(listOf(2, 2), sets.map { it.rir })
    }

    @Test
    fun startingWhileAWorkoutRunsResumesIt() = runTest(UnconfinedTestDispatcher()) {
        val running = workouts.startWorkout(now)

        vm.start(2L) {}

        assertEquals(running, workouts.current!!.workout.id)
        assertEquals(null, workouts.current!!.workout.routineId)
    }
}
