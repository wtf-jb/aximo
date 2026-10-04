package io.github.wtfjb.aximo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.github.wtfjb.aximo.domain.backup.BackupException
import io.github.wtfjb.aximo.domain.backup.BackupRepository
import io.github.wtfjb.aximo.domain.backup.SetsCsv
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/** Result of the last export or import, shown below the backup buttons. */
enum class BackupMessage { EXPORTED, CSV_EXPORTED, RESTORED, INVALID_FILE, NEWER_VERSION, FAILED }

data class BackupState(val busy: Boolean = false, val message: BackupMessage? = null)

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val training: TrainingSettings = TrainingSettings(),
    val backup: BackupState = BackupState(),
    /** How many standard exercises the last tap added, null before the first tap. */
    val catalogAdded: Int? = null,
    /** The active AI provider profile, null without one (B-01). */
    val aiProfile: AiProviderProfile? = null,
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
    aiProfiles: AiProfileRepository,
) : ViewModel() {

    private val backup = MutableStateFlow(BackupState())
    private val catalogAdded = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.themeMode,
        settings.training,
        backup,
        catalogAdded,
        aiProfiles.observeProfiles(),
    ) { theme, training, backup, added, profiles ->
        SettingsUiState(theme, training, backup, added, AiProfiles.active(profiles))
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
