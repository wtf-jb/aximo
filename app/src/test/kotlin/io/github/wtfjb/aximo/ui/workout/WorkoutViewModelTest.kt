package io.github.wtfjb.aximo.ui.workout

import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.rest.NextSet
import io.github.wtfjb.aximo.domain.rest.RestTimerController
import io.github.wtfjb.aximo.domain.rest.RestTimerEffects
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val start = Instant.fromEpochSeconds(1_790_000_000)
    private var now = start + 10.minutes
    private val time = TimeSource { now }

    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, repRangeMin = 6, repRangeMax = 8, restSeconds = 150)
    private val row = Exercise(id = 2, name = "Rudern", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, restSeconds = 90)
    private val history = mapOf(
        1L to listOf(
            SetEntry(id = 900, workoutExerciseId = 90, position = 0, weightKg = 80.0, reps = 8, completedAt = start),
            SetEntry(id = 901, workoutExerciseId = 90, position = 1, weightKg = 80.0, reps = 7, completedAt = start),
        ),
    )

    private val workouts = FakeWorkoutRepository(mapOf(1L to bench, 2L to row), history)

    private val noEffects = object : RestTimerEffects {
        override fun started() = Unit
        override fun finished(next: NextSet?) = Unit
    }
    private lateinit var restTimer: RestTimerController
    private val routines = io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository()

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val progression = FakeProgressionRepository()
    private val settings = FakeSettingsRepository()
    private val finisher by lazy { io.github.wtfjb.aximo.domain.workout.WorkoutFinisher(workouts, routines, progression, time, settings) }
    private val vm by lazy {
        WorkoutViewModel(workouts, FakeExerciseRepository(listOf(bench, row)), routines, progression, finisher, time, restTimer, settings)
    }

    private fun TestScope.started() {
        restTimer = RestTimerController(time, backgroundScope, noEffects)
        vm.uiState.launchIn(backgroundScope)
    }

    @Test
    fun noRunningWorkoutMeansTheScreenCloses() = runTest(UnconfinedTestDispatcher()) {
        started()
        assertEquals(false, vm.uiState.value.loading)
        assertNull(vm.uiState.value.workout)
    }

    @Test
    fun addedExerciseIsPrefilledFromTheLastSession() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()

        vm.addExercises(listOf(1L), superset = false)

        val sets = vm.uiState.value.workout!!.exercises.single().sets
        assertEquals(listOf(80.0, 80.0), sets.map { it.weightKg })
        assertEquals(listOf(8, 7), sets.map { it.reps })
        assertEquals(listOf(80.0, 80.0), vm.uiState.value.lastPerformance[1L]!!.map { it.weightKg })
        assertEquals(600L, vm.uiState.value.elapsedSeconds)
    }

    @Test
    fun completingASetMovesTheActiveRow() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()
        vm.addExercises(listOf(1L), superset = false)
        val first = vm.uiState.value.workout!!.exercises.single().sets[0]
        assertEquals(first.id, vm.uiState.value.groups.single().activeSetId)

        vm.toggleSetDone(first)

        val sets = vm.uiState.value.workout!!.exercises.single().sets
        assertEquals(now, sets[0].completedAt)
        assertEquals(sets[1].id, vm.uiState.value.groups.single().activeSetId)
    }

    @Test
    fun supersetGetsALetterAndAlternates() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()

        vm.addExercises(listOf(1L, 2L), superset = true)

        val group = vm.uiState.value.groups.single()
        assertEquals("A", group.supersetGroup)
        val benchSet = group.exercises[0].sets[0]
        vm.toggleSetDone(benchSet)
        val after = vm.uiState.value.groups.single()
        assertEquals(after.exercises[1].sets[0].id, after.activeSetId)
    }

    @Test
    fun editingParsesCommaAndClearsRir() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()
        vm.addExercises(listOf(2L), superset = false)
        val set = vm.uiState.value.workout!!.exercises.single().sets[0]

        vm.editSet(set, SetField.WEIGHT, "62,5")
        vm.editSet(vm.uiState.value.workout!!.exercises.single().sets[0], SetField.RIR, "2")
        vm.editSet(vm.uiState.value.workout!!.exercises.single().sets[0], SetField.REPS, "abc")

        var edited = vm.uiState.value.workout!!.exercises.single().sets[0]
        assertEquals(62.5, edited.weightKg, 0.0)
        assertEquals(2, edited.rir)
        assertEquals(set.reps, edited.reps)

        vm.editSet(edited, SetField.RIR, "")
        edited = vm.uiState.value.workout!!.exercises.single().sets[0]
        assertNull(edited.rir)

        // RPE replaces RIR and the other way round; out of range is ignored.
        vm.editSet(edited, SetField.RPE, "8,5")
        edited = vm.uiState.value.workout!!.exercises.single().sets[0]
        assertEquals(8.5, edited.rpe!!, 0.0)
        vm.editSet(edited, SetField.RPE, "11")
        assertEquals(8.5, vm.uiState.value.workout!!.exercises.single().sets[0].rpe!!, 0.0)
        vm.editSet(edited, SetField.RIR, "1")
        edited = vm.uiState.value.workout!!.exercises.single().sets[0]
        assertEquals(1, edited.rir)
        assertNull(edited.rpe)
    }

    @Test
    fun addSetCopiesTheLastSet() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()
        vm.addExercises(listOf(1L), superset = false)

        vm.addSet(vm.uiState.value.workout!!.exercises.single())

        val sets = vm.uiState.value.workout!!.exercises.single().sets
        assertEquals(3, sets.size)
        assertEquals(80.0, sets[2].weightKg, 0.0)
        assertEquals(7, sets[2].reps)
    }

    @Test
    fun finishingEndsTheWorkout() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()

        vm.finish()

        assertEquals(FinishState.Done(1L), vm.finishState.value)
        assertNull(vm.uiState.value.workout)
        assertTrue(!vm.uiState.value.loading)
    }

    @Test
    fun finishingRunsTheProgressionRules() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()
        vm.addExercises(listOf(1L), superset = false) // 80 × 8, 80 × 7 from history; range 6–8
        val sets = vm.uiState.value.workout!!.exercises.single().sets
        vm.editSet(sets[1], SetField.REPS, "8")
        vm.uiState.value.workout!!.exercises.single().sets.forEach { vm.toggleSetDone(it) }

        vm.finish()

        val state = progression.states.getValue(1L)
        assertEquals(io.github.wtfjb.aximo.domain.model.ProgressionReason.INCREASE_WEIGHT, state.reason)
        assertEquals(82.5, state.nextWeightKg, 0.0)
    }

    @Test
    fun addedExerciseUsesTheProgressionSuggestion() = runTest(UnconfinedTestDispatcher()) {
        progression.save(
            io.github.wtfjb.aximo.domain.model.ProgressionState(1, 82.5, 6, io.github.wtfjb.aximo.domain.model.ProgressionReason.INCREASE_WEIGHT),
        )
        workouts.startWorkout(start)
        started()

        vm.addExercises(listOf(1L), superset = false)

        val sets = vm.uiState.value.workout!!.exercises.single().sets
        assertEquals(listOf(82.5, 82.5), sets.map { it.weightKg })
        assertEquals(listOf(6, 6), sets.map { it.reps })
        assertEquals(82.5, vm.uiState.value.progression.getValue(1L).nextWeightKg, 0.0)
    }

    @Test
    fun completingASetStartsTheRestWithTheNextSet() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()
        vm.addExercises(listOf(1L), superset = false)
        assertNull(vm.uiState.value.rest)

        vm.toggleSetDone(vm.uiState.value.workout!!.exercises.single().sets[0])

        val rest = vm.uiState.value.rest!!
        assertEquals(150L, rest.remainingSeconds)
        assertEquals(1f, rest.remainingFraction, 0f)
        assertEquals(NextSet("Bankdrücken", 2), rest.next)

        vm.extendRest()
        assertEquals(165L, vm.uiState.value.rest!!.remainingSeconds)
        vm.skipRest()
        assertNull(vm.uiState.value.rest)
    }

    @Test
    fun reopeningASetStartsNoRest() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()
        vm.addExercises(listOf(1L), superset = false)
        vm.toggleSetDone(vm.uiState.value.workout!!.exercises.single().sets[0])
        vm.skipRest()

        vm.toggleSetDone(vm.uiState.value.workout!!.exercises.single().sets[0])

        assertNull(vm.uiState.value.workout!!.exercises.single().sets[0].completedAt)
        assertNull(vm.uiState.value.rest)
    }

    @Test
    fun supersetRestsAfterTheRound() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()
        vm.addExercises(listOf(1L, 2L), superset = true)

        vm.toggleSetDone(vm.uiState.value.groups.single().exercises[0].sets[0])
        assertNull(vm.uiState.value.rest)

        vm.toggleSetDone(vm.uiState.value.groups.single().exercises[1].sets[0])
        val rest = vm.uiState.value.rest
        assertNotNull(rest)
        assertEquals(150L, rest!!.remainingSeconds)
        assertEquals(NextSet("Bankdrücken", 2), rest.next)
    }

    @Test
    fun finishingStopsTheRest() = runTest(UnconfinedTestDispatcher()) {
        workouts.startWorkout(start)
        started()
        vm.addExercises(listOf(1L), superset = false)
        vm.toggleSetDone(vm.uiState.value.workout!!.exercises.single().sets[0])

        vm.finish()

        assertNull(restTimer.state.value)
    }

    @Test
    fun routineWorkoutShowsNameAndTargets() = runTest(UnconfinedTestDispatcher()) {
        val routineId = routines.saveRoutine(
            io.github.wtfjb.aximo.domain.model.Routine(name = "Push A"),
            listOf(io.github.wtfjb.aximo.domain.model.RoutineExercise(exerciseId = 1, position = 0, targetSets = 3, repMin = 6, repMax = 8, targetRir = 2)),
        )
        workouts.startWorkout(start, routineId)
        started()

        assertEquals("Push A", vm.uiState.value.routineName)
        assertEquals(3, vm.uiState.value.targets[1L]!!.targetSets)
    }
}
