package io.github.wtfjb.aximo.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.review.WeeklyReviewSetting
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek

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
            weeklyGoal = prefs[WEEKLY_GOAL]?.takeIf { it in TrainingSettings.WEEKLY_GOAL_CHOICES },
            bodyWeightKg = prefs[BODY_WEIGHT_KG]?.takeIf { it in TrainingSettings.BODY_WEIGHT_RANGE_KG },
        )
    }

    override suspend fun setTraining(settings: TrainingSettings) {
        dataStore.edit { prefs ->
            prefs[WEIGHT_UNIT] = settings.unit.name
            prefs[REST_SECONDS] = settings.restSeconds
            prefs[STEP_BARBELL_KG] = settings.steps.barbellKg
            prefs[STEP_DUMBBELL_KG] = settings.steps.dumbbellKg
            prefs[SET_RATING] = settings.rating.name
            // 0 = no goal (DataStore can't store null).
            prefs[WEEKLY_GOAL] = settings.weeklyGoal ?: 0
            prefs[BODY_WEIGHT_KG] = settings.bodyWeightKg ?: 0.0
        }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val REST_SECONDS = intPreferencesKey("default_rest_seconds")
        val STEP_BARBELL_KG = doublePreferencesKey("step_barbell_kg")
        val STEP_DUMBBELL_KG = doublePreferencesKey("step_dumbbell_kg")
        val SET_RATING = stringPreferencesKey("set_rating")
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val BODY_WEIGHT_KG = doublePreferencesKey("body_weight_kg")
    }
}

/** AI settings in the same DataStore file as the other settings. */
class DataStoreAiPreferences(context: Context) : AiPreferences {

    private val dataStore = context.settingsDataStore

    override val dataNoticeAccepted: Flow<Boolean> = dataStore.data.map { prefs -> prefs[AI_NOTICE_ACCEPTED] ?: false }

    override suspend fun acceptDataNotice() {
        dataStore.edit { prefs -> prefs[AI_NOTICE_ACCEPTED] = true }
    }

    override val chatNoticeAccepted: Flow<Boolean> = dataStore.data.map { prefs -> prefs[AI_CHAT_NOTICE_ACCEPTED] ?: false }

    override suspend fun acceptChatNotice() {
        dataStore.edit { prefs ->
            prefs[AI_CHAT_NOTICE_ACCEPTED] = true
            prefs[AI_NOTICE_ACCEPTED] = true
        }
    }

    override val weeklyReview: Flow<WeeklyReviewSetting> = dataStore.data.map { prefs ->
        val defaults = WeeklyReviewSetting()
        WeeklyReviewSetting(
            enabled = prefs[REVIEW_ENABLED] ?: defaults.enabled,
            day = DayOfWeek.entries.firstOrNull { it.name == prefs[REVIEW_DAY] } ?: defaults.day,
            hour = prefs[REVIEW_HOUR]?.takeIf { it in 0..23 } ?: defaults.hour,
        )
    }

    override suspend fun setWeeklyReview(setting: WeeklyReviewSetting) {
        dataStore.edit { prefs ->
            prefs[REVIEW_ENABLED] = setting.enabled
            prefs[REVIEW_DAY] = setting.day.name
            prefs[REVIEW_HOUR] = setting.hour
        }
    }

    private companion object {
        val AI_NOTICE_ACCEPTED = booleanPreferencesKey("ai_data_notice_accepted")
        val AI_CHAT_NOTICE_ACCEPTED = booleanPreferencesKey("ai_chat_notice_accepted")
        val REVIEW_ENABLED = booleanPreferencesKey("ai_weekly_review_enabled")
        val REVIEW_DAY = stringPreferencesKey("ai_weekly_review_day")
        val REVIEW_HOUR = intPreferencesKey("ai_weekly_review_hour")
    }
}
