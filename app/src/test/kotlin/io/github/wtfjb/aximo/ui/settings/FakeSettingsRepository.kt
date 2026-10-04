package io.github.wtfjb.aximo.ui.settings

import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory settings for ViewModel tests. */
class FakeSettingsRepository(training: TrainingSettings = TrainingSettings()) : SettingsRepository {
    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val trainingState = MutableStateFlow(training)
    override val training: StateFlow<TrainingSettings> = trainingState

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override suspend fun setTraining(settings: TrainingSettings) {
        trainingState.value = settings
    }
}
