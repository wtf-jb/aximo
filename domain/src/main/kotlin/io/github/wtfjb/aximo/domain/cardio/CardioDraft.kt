package io.github.wtfjb.aximo.domain.cardio

import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.model.CardioEntry
import kotlin.time.Instant

/** What is missing before a cardio entry can be saved. */
enum class CardioFieldError { ACTIVITY_MISSING, DURATION_MISSING }

/**
 * The "Cardio erfassen" form (A-04). Values are already parsed; the entry
 * dialogs use the parse functions below and keep the old value on bad input.
 */
data class CardioDraft(
    val exerciseId: Long? = null,
    val startedAt: Instant,
    val durationSec: Int? = null,
    val distanceM: Double? = null,
    val avgHeartRate: Int? = null,
    val elevationM: Double? = null,
    val note: String = "",
) {
    fun validate(): Set<CardioFieldError> = buildSet {
        if (exerciseId == null) add(CardioFieldError.ACTIVITY_MISSING)
        if (durationSec == null || durationSec <= 0) add(CardioFieldError.DURATION_MISSING)
    }

    /**
     * The entry to store, or null if [validate] finds errors. Fields the form
     * doesn't show (workout, source, external id) are taken from [original].
     */
    fun toEntry(original: CardioEntry? = null): CardioEntry? {
        if (validate().isNotEmpty()) return null
        val entry = CardioEntry(
            exerciseId = exerciseId!!,
            startedAt = startedAt,
            durationSec = durationSec!!,
            distanceM = distanceM,
            avgHeartRate = avgHeartRate,
            elevationM = elevationM,
            note = note.trim(),
        )
        return if (original == null) {
            entry
        } else {
            entry.copy(
                id = original.id,
                workoutId = original.workoutId,
                source = original.source,
                externalId = original.externalId,
            )
        }
    }

    companion object {
        fun from(entry: CardioEntry) = CardioDraft(
            exerciseId = entry.exerciseId,
            startedAt = entry.startedAt,
            durationSec = entry.durationSec,
            distanceM = entry.distanceM,
            avgHeartRate = entry.avgHeartRate,
            elevationM = entry.elevationM,
            note = entry.note,
        )

        /** "5,2" km → 5200.0 m. Null for anything that isn't a positive number. */
        fun parseDistanceM(km: String): Double? =
            ExerciseDraft.parseNumber(km)?.takeIf { it > 0 }?.let { it * 1000 }

        /** Average heart rate in bpm, plausible range 20–250. */
        fun parseHeartRate(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 20..250 }

        /** Elevation gain in metres, ≥ 0. */
        fun parseElevationM(text: String): Double? = ExerciseDraft.parseNumber(text)?.takeIf { it >= 0 }

        /** Distance in km for the entry dialog: 5200.0 → "5.2". */
        fun formatKm(distanceM: Double): String = ExerciseDraft.formatNumber(distanceM / 1000)
    }
}
