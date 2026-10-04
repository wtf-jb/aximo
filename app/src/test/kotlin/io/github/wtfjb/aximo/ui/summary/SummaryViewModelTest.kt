package io.github.wtfjb.aximo.ui.summary

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.stats.RecordType
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.workout.FakeProgressionRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SummaryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val start = Instant.fromEpochSeconds(1_790_000_000)
    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val history = mapOf(1L to listOf(SetEntry(workoutExerciseId = 50, position = 0, weightKg = 80.0, reps = 6, completedAt = start)))
    private val workouts = FakeWorkoutRepository(mapOf(1L to bench), history)
    private val progression = FakeProgressionRepository(listOf(ProgressionState(1, 87.5, 6, ProgressionReason.INCREASE_WEIGHT)))

    private suspend fun finishedWorkout(): Long {
        val id = workouts.startWorkout(start)
        val weId = workouts.addExercise(id, 1, null, listOf(PlannedSet(85.0, 6), PlannedSet(85.0, 6)))
        workouts.current!!.exercises.single().sets.forEach { workouts.updateSet(it.copy(completedAt = start)) }
        workouts.finishWorkout(id, start + 54.minutes, "")
        return id.also { check(weId > 0) }
    }

    @Test
    fun showsStatsRecordsAndNextTime() = runTest {
        val id = finishedWorkout()
        val vm = SummaryViewModel(workouts, FakeRoutineRepository(), progression, id)
        val state = vm.uiState.value

        assertEquals(54 * 60L, state.durationSeconds)
        assertEquals(1020.0, state.volumeKg, 0.0)
        assertEquals(2, state.workingSets)
        assertEquals(RecordType.E1RM, state.records.single().record.type)
        assertEquals(85.0, state.nextTime.single().currentWeightKg, 0.0)
        assertEquals(87.5, state.nextTime.single().suggestion.nextWeightKg, 0.0)
    }

    @Test
    fun savingStoresRatingAndNote() = runTest {
        val id = finishedWorkout()
        val vm = SummaryViewModel(workouts, FakeRoutineRepository(), progression, id)

        vm.onRatingChange(4)
        vm.onNoteChange("stark")
        vm.save()

        assertTrue(vm.uiState.value.done)
        assertEquals(4, workouts.finished.getValue(id).workout.rating)
        assertEquals("stark", workouts.finished.getValue(id).workout.note)
    }

    @Test
    fun comparesVolumeWithThePreviousWorkoutOfTheRoutine() = runTest {
        val routines = FakeRoutineRepository()
        val routineId = routines.saveRoutine(io.github.wtfjb.aximo.domain.model.Routine(name = "Push A"), emptyList())
        workouts.allFinished.value = listOf(io.github.wtfjb.aximo.ui.stats.StatsTestWorkouts.workout(90, 30, 100.0, 10, routineId))
        val id = workouts.startWorkout(start, routineId)
        workouts.addExercise(id, 1, null, listOf(PlannedSet(110.0, 10)))
        workouts.current!!.exercises.single().sets.forEach { workouts.updateSet(it.copy(completedAt = start)) }
        workouts.finishWorkout(id, start + 50.minutes, "")

        val vm = SummaryViewModel(workouts, routines, progression, id)

        assertEquals(0.1, vm.uiState.value.volumeChange!!, 1e-9)
        assertEquals("Push A", vm.uiState.value.routineName)
    }
}
