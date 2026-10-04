package io.github.wtfjb.aximo.ui.exercises

import io.github.wtfjb.aximo.domain.exercise.ExerciseFieldError
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExerciseEditViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val stored = Exercise(
        id = 3,
        name = "Bankdrücken",
        type = ExerciseType.STRENGTH,
        equipment = Equipment.BARBELL,
        primaryMuscles = setOf(MuscleGroup.CHEST),
        roundingStepKg = 1.25,
    )

    @Test
    fun errorsAppearOnlyAfterTheFirstSaveAttempt() {
        val vm = ExerciseEditViewModel(FakeExerciseRepository(), exerciseId = 0)
        assertEquals(emptySet<ExerciseFieldError>(), vm.uiState.value.errors)

        vm.save()

        assertEquals(setOf(ExerciseFieldError.NAME_MISSING), vm.uiState.value.errors)
        assertFalse(vm.uiState.value.done)
    }

    @Test
    fun newExerciseIsSavedAndScreenCloses() {
        val repo = FakeExerciseRepository()
        val vm = ExerciseEditViewModel(repo, exerciseId = 0)

        vm.onDraftChange { it.copy(name = "Klimmzug", type = ExerciseType.BODYWEIGHT).togglePrimary(MuscleGroup.LATS) }
        vm.save()

        assertTrue(vm.uiState.value.done)
        assertEquals("Klimmzug", repo.all.single().name)
        assertEquals(setOf(MuscleGroup.LATS), repo.all.single().primaryMuscles)
    }

    @Test
    fun editingLoadsTheExerciseAndKeepsHiddenFields() {
        val repo = FakeExerciseRepository(listOf(stored))
        val vm = ExerciseEditViewModel(repo, exerciseId = 3)
        assertEquals("Bankdrücken", vm.uiState.value.draft.name)
        assertFalse(vm.uiState.value.isNew)

        vm.onDraftChange { it.copy(name = "Bankdrücken eng") }
        vm.save()

        val saved = repo.all.single()
        assertEquals("Bankdrücken eng", saved.name)
        assertEquals(1.25, saved.roundingStepKg!!, 0.0)
    }

    @Test
    fun archivingSavesAndCloses() {
        val repo = FakeExerciseRepository(listOf(stored))
        val vm = ExerciseEditViewModel(repo, exerciseId = 3)

        vm.toggleArchived()

        assertTrue(repo.all.single().archived)
        assertTrue(vm.uiState.value.done)
    }
}
