package io.github.wtfjb.aximo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.units.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state: theme choice and weight display unit. */
class MainViewModel(private val settings: SettingsRepository) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM,
    )

    val weightUnit: StateFlow<WeightUnit> = settings.training.map { it.unit }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightUnit.KG,
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}
