package io.github.wtfjb.aximo.ui.routine

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoutineEditViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun ex(id: Long) = Exercise(id = id, name = "E$id", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val exercises = FakeExerciseRepository(listOf(ex(1), ex(2), ex(3)))

    private val existing = RoutineWithExercises(
        Routine(id = 7, name = "Push", position = 0),
        listOf(RoutineExercise(id = 1, routineId = 7, exerciseId = 1, position = 0, targetSets = 3, repMin = 6, repMax = 8, targetRir = 2)),
    )

    @Test
    fun newRoutineNeedsANameAndGoesToTheEnd() {
        val routines = FakeRoutineRepository(listOf(existing))
        val vm = RoutineEditViewModel(routines, exercises, routineId = 0)

        vm.save()
        assertTrue(vm.uiState.value.showErrors)
        assertFalse(vm.uiState.value.done)

        vm.onNameChange("Pull")
        vm.addExercises(listOf(2, 3), superset = true)
        vm.save()

        assertTrue(vm.uiState.value.done)
        val saved = routines.all.first { it.routine.name == "Pull" }
        assertEquals(1, saved.routine.position)
        assertEquals(listOf("A", "A"), saved.exercises.map { it.supersetGroup })
    }

    @Test
    fun editingKeepsTheRoutineAndChangesTargets() {
        val routines = FakeRoutineRepository(listOf(existing))
        val vm = RoutineEditViewModel(routines, exercises, routineId = 7)
        assertEquals("Push", vm.uiState.value.draft.name)

        vm.addExercises(listOf(2), superset = false)
        vm.updateTargets(1, sets = 4, repMin = 10, repMax = 12, rir = 1)
        vm.move(1, -1)
        vm.save()

        val saved = routines.all.single()
        assertEquals(listOf(2L, 1L), saved.exercises.map { it.exerciseId })
        assertEquals(4, saved.exercises[0].targetSets)
        assertEquals(listOf(0, 1), saved.exercises.map { it.position })
    }

    @Test
    fun deleteRemovesTheRoutine() {
        val routines = FakeRoutineRepository(listOf(existing))
        val vm = RoutineEditViewModel(routines, exercises, routineId = 7)

        vm.delete()

        assertEquals(emptyList<RoutineWithExercises>(), routines.all)
        assertTrue(vm.uiState.value.done)
    }
}
