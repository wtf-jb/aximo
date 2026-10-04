package io.github.wtfjb.aximo.ui.settings

import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import io.github.wtfjb.aximo.domain.backup.BackupException
import io.github.wtfjb.aximo.domain.backup.SetsCsv
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import io.github.wtfjb.aximo.domain.exercise.CatalogSeeder
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val backup = FakeBackupRepository()
    private val documents = FakeDocumentStore()
    private val workouts = FakeWorkoutRepository(emptyMap())
    private val exercises = FakeExerciseRepository()
    private val vm by lazy { SettingsViewModel(settings, backup, workouts, FakeRoutineRepository(), documents, CatalogSeeder(exercises, settings) { it.name }) }

    @Test
    fun showsTheStoredSettings() = runTest(UnconfinedTestDispatcher()) {
        settings.setTraining(TrainingSettings(restSeconds = 90))
        vm.uiState.launchIn(backgroundScope)

        assertEquals(90, vm.uiState.value.training.restSeconds)
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)
    }

    @Test
    fun switchingToLbsAlsoSwitchesStandardSteps() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)

        vm.setUnit(WeightUnit.LBS)

        assertEquals(WeightUnit.LBS, settings.training.value.unit)
        assertEquals(WeightSteps.standard(WeightUnit.LBS), settings.training.value.steps)
    }

    @Test
    fun restAndThemeAreStored() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)

        vm.setRestSeconds(180)
        vm.setThemeMode(ThemeMode.DARK)
        vm.setRating(SetRating.RPE)

        assertEquals(SetRating.RPE, settings.training.value.rating)
        assertEquals(180, settings.training.value.restSeconds)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }

    @Test
    fun stepsAreEnteredInTheDisplayUnit() = runTest(UnconfinedTestDispatcher()) {
        settings.setTraining(TrainingSettings().withUnit(WeightUnit.LBS))
        vm.uiState.launchIn(backgroundScope)

        assertTrue(vm.setSteps("2,5", "5"))

        assertEquals(2.5, WeightUnit.LBS.fromKg(settings.training.value.steps.barbellKg), 1e-9)
        assertEquals(5.0, WeightUnit.LBS.fromKg(settings.training.value.steps.dumbbellKg), 1e-9)
    }

    @Test
    fun invalidStepsAreRejected() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)

        assertFalse(vm.setSteps("0", "2"))
        assertFalse(vm.setSteps("abc", "2"))

        assertEquals(WeightSteps.standard(WeightUnit.KG), settings.training.value.steps)
    }

    @Test
    fun exportWritesTheBackupIntoThePickedFile() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)

        vm.exportJson("content://backup.json")

        assertEquals("{\"schemaVersion\": 1}", documents.files["content://backup.json"])
        assertEquals(BackupState(busy = false, message = BackupMessage.EXPORTED), vm.uiState.value.backup)
    }

    @Test
    fun csvExportWritesTheSets() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)

        vm.exportCsv("content://sets.csv")

        assertEquals(SetsCsv.HEADER.joinToString(",") + "\n", documents.files["content://sets.csv"])
        assertEquals(BackupMessage.CSV_EXPORTED, vm.uiState.value.backup.message)
    }

    @Test
    fun restoreReadsThePickedFile() = runTest(UnconfinedTestDispatcher()) {
        documents.files["content://old.json"] = "{ old }"
        vm.uiState.launchIn(backgroundScope)

        vm.restore("content://old.json")

        assertEquals("{ old }", backup.restored)
        assertEquals(BackupMessage.RESTORED, vm.uiState.value.backup.message)
    }

    @Test
    fun restoreErrorsBecomeMessages() = runTest(UnconfinedTestDispatcher()) {
        documents.files["content://x.json"] = "x"
        vm.uiState.launchIn(backgroundScope)

        backup.failWith = BackupException.Reason.INVALID_FILE
        vm.restore("content://x.json")
        assertEquals(BackupMessage.INVALID_FILE, vm.uiState.value.backup.message)

        backup.failWith = BackupException.Reason.NEWER_VERSION
        vm.restore("content://x.json")
        assertEquals(BackupMessage.NEWER_VERSION, vm.uiState.value.backup.message)

        vm.restore("content://missing.json")
        assertEquals(BackupMessage.FAILED, vm.uiState.value.backup.message)
        assertEquals(null, backup.restored)
    }

    @Test
    fun addingStandardExercisesReportsTheCount() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)

        vm.addStandardExercises()
        assertEquals(CatalogExercise.entries.size, vm.uiState.value.catalogAdded)

        vm.addStandardExercises()
        assertEquals(0, vm.uiState.value.catalogAdded)
    }
}
