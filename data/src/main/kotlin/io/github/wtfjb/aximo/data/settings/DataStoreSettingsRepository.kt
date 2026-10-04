package io.github.wtfjb.aximo.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Stores settings in a local DataStore file. Missing or invalid values fall back to the defaults. */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val dataStore = context.settingsDataStore

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val stored = prefs[THEME_MODE]
        ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME_MODE] = mode.name }
    }

    override val training: Flow<TrainingSettings> = dataStore.data.map { prefs ->
        val unit = WeightUnit.entries.firstOrNull { it.name == prefs[WEIGHT_UNIT] } ?: WeightUnit.KG
        val standard = WeightSteps.standard(unit)
        val barbell = prefs[STEP_BARBELL_KG]?.takeIf { it > 0 } ?: standard.barbellKg
        val dumbbell = prefs[STEP_DUMBBELL_KG]?.takeIf { it > 0 } ?: standard.dumbbellKg
        val defaults = TrainingSettings(unit = unit)
        TrainingSettings(
            unit = unit,
            restSeconds = prefs[REST_SECONDS]?.takeIf { it >= 0 } ?: defaults.restSeconds,
            steps = WeightSteps(barbellKg = barbell, dumbbellKg = dumbbell),
            rating = SetRating.entries.firstOrNull { it.name == prefs[SET_RATING] } ?: SetRating.RIR,
        )
    }

    override suspend fun setTraining(settings: TrainingSettings) {
        dataStore.edit { prefs ->
            prefs[WEIGHT_UNIT] = settings.unit.name
            prefs[REST_SECONDS] = settings.restSeconds
            prefs[STEP_BARBELL_KG] = settings.steps.barbellKg
            prefs[STEP_DUMBBELL_KG] = settings.steps.dumbbellKg
            prefs[SET_RATING] = settings.rating.name
        }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val REST_SECONDS = intPreferencesKey("default_rest_seconds")
        val STEP_BARBELL_KG = doublePreferencesKey("step_barbell_kg")
        val STEP_DUMBBELL_KG = doublePreferencesKey("step_dumbbell_kg")
        val SET_RATING = stringPreferencesKey("set_rating")
    }
}
