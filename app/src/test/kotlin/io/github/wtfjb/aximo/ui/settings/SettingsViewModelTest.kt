package io.github.wtfjb.aximo.ui.settings

import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
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
    private val vm by lazy { SettingsViewModel(settings) }

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
}
