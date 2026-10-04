package io.github.wtfjb.aximo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.github.wtfjb.aximo.domain.backup.BackupException
import io.github.wtfjb.aximo.domain.review.ReviewContext
import io.github.wtfjb.aximo.domain.review.ReviewService
import io.github.wtfjb.aximo.domain.review.WeeklyReviewSetting
import io.github.wtfjb.aximo.domain.backup.BackupRepository
import io.github.wtfjb.aximo.domain.backup.SetsCsv
import io.github.wtfjb.aximo.domain.devdata.DevDataRepository
import io.github.wtfjb.aximo.domain.exercise.CatalogSeeder
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DayOfWeek
import kotlinx.coroutines.launch
import java.io.IOException

/** Result of the last export or import, shown below the backup buttons. */
enum class BackupMessage { EXPORTED, CSV_EXPORTED, RESTORED, INVALID_FILE, NEWER_VERSION, FAILED }

data class BackupState(val busy: Boolean = false, val message: BackupMessage? = null)

/** Result of the developer tools (debug builds only). */
enum class DevMessage { LOADED, CLEARED, FAILED }

data class DevState(val busy: Boolean = false, val message: DevMessage? = null)

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val training: TrainingSettings = TrainingSettings(),
    val backup: BackupState = BackupState(),
    val dev: DevState = DevState(),
    /** How many standard exercises the last tap added, null before the first tap. */
    val catalogAdded: Int? = null,
    /** The active AI provider profile, null without one (B-01). */
    val aiProfile: AiProviderProfile? = null,
    val ai: AiSettingsState = AiSettingsState(),
)

/** Weekly review settings and their dialogs (B-02). */
data class AiSettingsState(
    val weeklyReview: WeeklyReviewSetting = WeeklyReviewSetting(),
    /** Turning the weekly review on first needs the data notice. */
    val showNotice: Boolean = false,
    /** Pretty JSON of what a review sends, while the dialog is open. */
    val sentData: String? = null,
)

/**
 * Settings screen (A-09): unit, theme, default rest and weight steps. Language is
 * handled by Android. Also export and import of all data (A-08) and the entry
 * to the AI provider profiles (B-01).
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val documents: DocumentStore,
    private val catalog: CatalogSeeder,
    private val devData: DevDataRepository,
    aiProfiles: AiProfileRepository,
    private val aiPreferences: AiPreferences,
    private val reviewService: ReviewService,
    /** Renders the review context as sent (pretty JSON). */
    private val formatContext: (ReviewContext) -> String,
) : ViewModel() {

    private val aiDialogs = MutableStateFlow(AiSettingsState())
    private val ai = combine(aiPreferences.weeklyReview, aiDialogs) { setting, dialogs -> dialogs.copy(weeklyReview = setting) }

    private val backup = MutableStateFlow(BackupState())
    private val dev = MutableStateFlow(DevState())
    private val catalogAdded = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.themeMode,
        settings.training,
        combine(backup, dev) { backup, dev -> backup to dev },
        catalogAdded,
        combine(aiProfiles.observeProfiles(), ai) { profiles, ai -> AiProfiles.active(profiles) to ai },
    ) { theme, training, (backup, dev), added, (profile, ai) ->
        SettingsUiState(theme, training, backup, dev, added, profile, ai)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setUnit(unit: WeightUnit) = updateTraining { it.withUnit(unit) }

    fun setRestSeconds(seconds: Int) = updateTraining { it.copy(restSeconds = seconds) }

    fun setRating(rating: SetRating) = updateTraining { it.copy(rating = rating) }

    /** null = no goal. */
    fun setWeeklyGoal(goal: Int?) = updateTraining { it.copy(weeklyGoal = goal) }

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

    /** Switch "Wöchentlicher Review". Turning it on the first time shows the data notice. */
    fun setWeeklyReviewEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !aiPreferences.dataNoticeAccepted.first()) {
                aiDialogs.update { it.copy(showNotice = true) }
            } else {
                aiPreferences.setWeeklyReview(aiPreferences.weeklyReview.first().copy(enabled = enabled))
            }
        }
    }

    fun acceptNotice() {
        aiDialogs.update { it.copy(showNotice = false) }
        viewModelScope.launch {
            aiPreferences.acceptDataNotice()
            aiPreferences.setWeeklyReview(aiPreferences.weeklyReview.first().copy(enabled = true))
        }
    }

    fun dismissNotice() = aiDialogs.update { it.copy(showNotice = false) }

    fun setWeeklyReviewTime(day: DayOfWeek, hour: Int) {
        viewModelScope.launch { aiPreferences.setWeeklyReview(aiPreferences.weeklyReview.first().copy(day = day, hour = hour)) }
    }

    fun showSentData() {
        viewModelScope.launch {
            val text = formatContext(reviewService.context())
            aiDialogs.update { it.copy(sentData = text) }
        }
    }

    fun dismissSentData() = aiDialogs.update { it.copy(sentData = null) }

    /** Adds the catalog exercises that are missing (A-01). */
    fun addStandardExercises() {
        viewModelScope.launch { catalogAdded.value = catalog.addMissing() }
    }

    /** Full backup as JSON into the file the user created. */
    fun exportJson(uri: String) = runBackup(BackupMessage.EXPORTED) {
        documents.write(uri, backupRepository.exportJson())
    }

    /** All sets of finished workouts as CSV. */
    fun exportCsv(uri: String) = runBackup(BackupMessage.CSV_EXPORTED) {
        documents.write(uri, SetsCsv.build(workouts.observeFinished().first(), routines.observeRoutines().first()))
    }

    /** Replaces all data with the backup in the picked file. The screen asks for confirmation first. */
    fun restore(uri: String) = runBackup(BackupMessage.RESTORED) {
        backupRepository.restoreJson(documents.read(uri))
    }

    /** Debug tool: replaces all training data with generated sample data. The screen asks first. */
    fun loadTestData() = runDev(DevMessage.LOADED) { devData.loadTestData() }

    /** Debug tool: deletes all user data. The screen asks first. */
    fun clearAllData() = runDev(DevMessage.CLEARED) { devData.clearAll() }

    private fun runDev(success: DevMessage, action: suspend () -> Unit) {
        if (dev.value.busy) return
        dev.value = DevState(busy = true)
        viewModelScope.launch {
            val message = try {
                action()
                success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DevMessage.FAILED
            }
            dev.value = DevState(busy = false, message = message)
        }
    }

    private fun runBackup(success: BackupMessage, action: suspend () -> Unit) {
        if (backup.value.busy) return
        backup.value = BackupState(busy = true)
        viewModelScope.launch {
            val message = try {
                action()
                success
            } catch (e: BackupException) {
                when (e.reason) {
                    BackupException.Reason.INVALID_FILE -> BackupMessage.INVALID_FILE
                    BackupException.Reason.NEWER_VERSION -> BackupMessage.NEWER_VERSION
                }
            } catch (e: IOException) {
                BackupMessage.FAILED
            } catch (e: SecurityException) {
                // The file permission was revoked or the provider refused it.
                BackupMessage.FAILED
            }
            backup.value = BackupState(busy = false, message = message)
        }
    }

    /** Reads the stored value first, so quick taps never work on a stale copy. */
    private fun updateTraining(change: (TrainingSettings) -> TrainingSettings) {
        viewModelScope.launch { settings.setTraining(change(settings.training.first())) }
    }
}
