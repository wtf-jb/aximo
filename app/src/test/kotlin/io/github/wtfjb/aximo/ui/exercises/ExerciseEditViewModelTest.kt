package io.github.wtfjb.aximo.ui.exercises

import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import io.github.wtfjb.aximo.domain.exercise.ExerciseFieldError
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.units.WeightUnit
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
        val vm = ExerciseEditViewModel(FakeExerciseRepository(), FakeSettingsRepository(), exerciseId = 0)
        assertEquals(emptySet<ExerciseFieldError>(), vm.uiState.value.errors)

        vm.save()

        assertEquals(setOf(ExerciseFieldError.NAME_MISSING), vm.uiState.value.errors)
        assertFalse(vm.uiState.value.done)
    }

    @Test
    fun newExerciseIsSavedAndScreenCloses() {
        val repo = FakeExerciseRepository()
        val vm = ExerciseEditViewModel(repo, FakeSettingsRepository(), exerciseId = 0)

        vm.onDraftChange { it.copy(name = "Klimmzug", type = ExerciseType.BODYWEIGHT).togglePrimary(MuscleGroup.LATS) }
        vm.save()

        assertTrue(vm.uiState.value.done)
        assertEquals("Klimmzug", repo.all.single().name)
        assertEquals(setOf(MuscleGroup.LATS), repo.all.single().primaryMuscles)
    }

    @Test
    fun editingLoadsTheExerciseAndKeepsHiddenFields() {
        val repo = FakeExerciseRepository(listOf(stored))
        val vm = ExerciseEditViewModel(repo, FakeSettingsRepository(), exerciseId = 3)
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
        val vm = ExerciseEditViewModel(repo, FakeSettingsRepository(), exerciseId = 3)

        vm.toggleArchived()

        assertTrue(repo.all.single().archived)
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun newExerciseStartsWithTheDefaultsFromTheSettings() {
        val settings = FakeSettingsRepository(TrainingSettings(restSeconds = 90).withUnit(WeightUnit.LBS))
        val vm = ExerciseEditViewModel(FakeExerciseRepository(), settings, exerciseId = 0)

        val draft = vm.uiState.value.draft
        assertEquals("90", draft.restSeconds)
        assertEquals("5", draft.increment)
        assertEquals(WeightUnit.LBS, draft.unit)
    }

    @Test
    fun choosingDumbbellsSwitchesTheDefaultIncrement() {
        val vm = ExerciseEditViewModel(FakeExerciseRepository(), FakeSettingsRepository(), exerciseId = 0)

        vm.onEquipmentChange(Equipment.DUMBBELL)

        assertEquals(Equipment.DUMBBELL, vm.uiState.value.draft.equipment)
        assertEquals("2", vm.uiState.value.draft.increment)
    }

    @Test
    fun editingShowsTheIncrementInTheDisplayUnit() {
        val repo = FakeExerciseRepository(listOf(stored.copy(incrementKg = WeightUnit.LBS.toKg(5.0))))
        val settings = FakeSettingsRepository(TrainingSettings().withUnit(WeightUnit.LBS))
        val vm = ExerciseEditViewModel(repo, settings, exerciseId = 3)

        assertEquals("5", vm.uiState.value.draft.increment)
    }
}
