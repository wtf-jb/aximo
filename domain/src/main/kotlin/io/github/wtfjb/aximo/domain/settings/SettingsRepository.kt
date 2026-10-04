package io.github.wtfjb.aximo.domain.settings

import kotlinx.coroutines.flow.Flow

/** App-wide user settings. Implemented in :data. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    val training: Flow<TrainingSettings>

    suspend fun setTraining(settings: TrainingSettings)
}
