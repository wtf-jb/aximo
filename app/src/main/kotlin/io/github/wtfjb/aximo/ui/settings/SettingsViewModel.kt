package io.github.wtfjb.aximo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val training: TrainingSettings = TrainingSettings(),
)

/** Settings screen (A-09): unit, theme, default rest and weight steps. Language is handled by Android. */
class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(settings.themeMode, settings.training) { theme, training ->
        SettingsUiState(theme, training)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setUnit(unit: WeightUnit) = updateTraining { it.withUnit(unit) }

    fun setRestSeconds(seconds: Int) = updateTraining { it.copy(restSeconds = seconds) }

    /**
     * Steps as typed in the display unit ("2,5", "2"). Returns false and changes
     * nothing if one of them is not a positive number.
     */
    fun setSteps(barbell: String, dumbbell: String): Boolean {
        val unit = uiState.value.training.unit
        val barbellValue = ExerciseDraft.parseNumber(barbell)?.takeIf { it > 0 } ?: return false
        val dumbbellValue = ExerciseDraft.parseNumber(dumbbell)?.takeIf { it > 0 } ?: return false
        val steps = WeightSteps(barbellKg = unit.toKg(barbellValue), dumbbellKg = unit.toKg(dumbbellValue))
        updateTraining { it.copy(steps = steps) }
        return true
    }

    /** Reads the stored value first, so quick taps never work on a stale copy. */
    private fun updateTraining(change: (TrainingSettings) -> TrainingSettings) {
        viewModelScope.launch { settings.setTraining(change(settings.training.first())) }
    }
}
