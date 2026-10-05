package io.github.wtfjb.aximo.ui.cardio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.calories.CalorieEstimate
import io.github.wtfjb.aximo.domain.cardio.CardioActivities
import io.github.wtfjb.aximo.domain.cardio.CardioDraft
import io.github.wtfjb.aximo.domain.cardio.CardioFieldError
import io.github.wtfjb.aximo.domain.cardio.CardioMath
import io.github.wtfjb.aximo.domain.cardio.DefaultActivity
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.repository.CardioRepository
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the "Cardio erfassen" screen shows. */
data class CardioUiState(
    val draft: CardioDraft,
    val activities: List<Exercise> = emptyList(),
    val isNew: Boolean = true,
    val loading: Boolean = true,
    /** Errors are only shown after the first save attempt. */
    val showErrors: Boolean = false,
    /** True once saved or deleted: the screen closes. */
    val done: Boolean = false,
    /** From the settings; null = no calorie estimate. */
    val bodyWeightKg: Double? = null,
) {
    val errors: Set<CardioFieldError> get() = if (showErrors) draft.validate() else emptySet()

    val paceStyle: PaceStyle
        get() = activities.firstOrNull { it.id == draft.exerciseId }?.let(CardioActivities::paceStyle) ?: PaceStyle.SPEED

    /** Seconds per km or per 500 m, depending on [paceStyle]; null without distance. */
    val paceSeconds: Double?
        get() {
            val duration = draft.durationSec ?: return null
            return if (paceStyle == PaceStyle.PER_500M) {
                CardioMath.paceSecondsPer500m(duration, draft.distanceM)
            } else {
                CardioMath.paceSecondsPerKm(duration, draft.distanceM)
            }
        }

    val speedKmh: Double? get() = draft.durationSec?.let { CardioMath.speedKmh(it, draft.distanceM) }

    /** Estimated kcal; null without body weight or duration. */
    val kcal: Int?
        get() {
            val weight = bodyWeightKg ?: return null
            val duration = draft.durationSec ?: return null
            val activity = activities.firstOrNull { it.id == draft.exerciseId }?.let(CardioActivities::defaultOf)
            return CalorieEstimate.cardio(activity, duration, draft.distanceM, weight)
        }
}

/**
 * Log (entryId = 0) or edit a cardio session (A-04). Creates the default
 * activities on first use; [defaultName] gives their localized names.
 */
class CardioViewModel(
    private val cardio: CardioRepository,
    private val exercises: ExerciseRepository,
    private val time: TimeSource,
    private val settings: SettingsRepository,
    private val defaultName: (DefaultActivity) -> String,
    entryId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardioUiState(draft = CardioDraft(startedAt = nowToTheMinute()), isNew = entryId == 0L))
    val uiState: StateFlow<CardioUiState> = _uiState.asStateFlow()

    /** The stored entry when editing, to keep fields the form doesn't show. */
    private var original: CardioEntry? = null

    init {
        viewModelScope.launch {
            val bodyWeightKg = settings.training.first().bodyWeightKg
            _uiState.update { it.copy(bodyWeightKg = bodyWeightKg) }
            var all = exercises.observeExercises(includeArchived = true).first()
            if (CardioActivities.needsDefaults(all)) {
                CardioActivities.defaults(defaultName).forEach { exercises.saveExercise(it) }
                all = exercises.observeExercises(includeArchived = true).first()
            }
            if (entryId != 0L) {
                val stored = cardio.getEntry(entryId)
                if (stored == null) {
                    _uiState.update { it.copy(done = true) }
                    return@launch
                }
                original = stored
                _uiState.update {
                    it.copy(
                        draft = CardioDraft.from(stored),
                        activities = CardioActivities.choices(all, keepId = stored.exerciseId),
                        loading = false,
                    )
                }
            } else {
                val choices = CardioActivities.choices(all)
                val latest = cardio.observeRecent(1).first().firstOrNull()
                _uiState.update { state ->
                    state.copy(
                        draft = state.draft.copy(exerciseId = CardioActivities.preselect(choices, latest)),
                        activities = choices,
                        loading = false,
                    )
                }
            }
        }
    }

    private fun edit(change: (CardioDraft) -> CardioDraft) {
        _uiState.update { it.copy(draft = change(it.draft)) }
    }

    fun onActivityChange(exerciseId: Long) = edit { it.copy(exerciseId = exerciseId) }

    fun onStartChange(startedAt: Instant) = edit { it.copy(startedAt = startedAt) }

    /** From the h / min / s fields; invalid input keeps the old value. */
    fun onDurationChange(hours: String, minutes: String, seconds: String) {
        val total = CardioMath.durationOf(hours, minutes, seconds) ?: return
        edit { it.copy(durationSec = total) }
    }

    /** Distance in km; blank clears it, invalid input keeps the old value. */
    fun onDistanceChange(km: String) {
        val meters = if (km.isBlank()) null else CardioDraft.parseDistanceM(km) ?: return
        edit { it.copy(distanceM = meters) }
    }

    fun onHeartRateChange(text: String) {
        val bpm = if (text.isBlank()) null else CardioDraft.parseHeartRate(text) ?: return
        edit { it.copy(avgHeartRate = bpm) }
    }

    fun onElevationChange(text: String) {
        val meters = if (text.isBlank()) null else CardioDraft.parseElevationM(text) ?: return
        edit { it.copy(elevationM = meters) }
    }

    fun onNoteChange(note: String) = edit { it.copy(note = note) }

    fun save() {
        val entry = _uiState.value.draft.toEntry(original)
        if (entry == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            cardio.saveEntry(entry)
            _uiState.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val id = original?.id ?: return
        viewModelScope.launch {
            cardio.deleteEntry(id)
            _uiState.update { it.copy(done = true) }
        }
    }

    private fun nowToTheMinute(): Instant = Instant.fromEpochSeconds(time.now().epochSeconds / 60 * 60)
}
