package io.github.wtfjb.aximo.domain.stats

import io.github.wtfjb.aximo.domain.cardio.CardioMath
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Exercise
import kotlin.math.roundToInt
import kotlin.time.Instant

/** What the cardio chart shows. PACE follows the pace style of the activity (pace or speed). */
enum class CardioMetric { PACE, DISTANCE, DURATION, HEART_RATE }

/**
 * One value per entry: pace in seconds (per km or per 500 m) or speed in km/h,
 * distance in m, duration in s, heart rate in bpm.
 */
data class CardioPoint(val entryId: Long, val startedAt: Instant, val value: Double)

data class CardioSummary(
    val sessions: Int,
    val distanceM: Double,
    val durationSec: Long,
    /** Mean of the entries that have a heart rate; null if none has. */
    val avgHeartRate: Int?,
)

/** Cardio trends per activity (A-07). */
object CardioTrends {

    /** Activities with at least one entry, most recently done first. */
    fun activities(entries: List<CardioEntry>, exercises: List<Exercise>): List<Exercise> {
        val byId = exercises.associateBy { it.id }
        return entries.sortedByDescending { it.startedAt }.mapNotNull { byId[it.exerciseId] }.distinctBy { it.id }
    }

    /** Points of the given entries, oldest first. Entries without the value (no distance, no heart rate) are left out. */
    fun points(entries: List<CardioEntry>, metric: CardioMetric, style: PaceStyle): List<CardioPoint> =
        entries.sortedBy { it.startedAt }.mapNotNull { entry ->
            value(entry, metric, style)?.let { CardioPoint(entry.id, entry.startedAt, it) }
        }

    fun value(entry: CardioEntry, metric: CardioMetric, style: PaceStyle): Double? = when (metric) {
        CardioMetric.PACE -> when (style) {
            PaceStyle.PER_KM -> CardioMath.paceSecondsPerKm(entry.durationSec, entry.distanceM)
            PaceStyle.PER_500M -> CardioMath.paceSecondsPer500m(entry.durationSec, entry.distanceM)
            PaceStyle.SPEED -> CardioMath.speedKmh(entry.durationSec, entry.distanceM)
        }
        CardioMetric.DISTANCE -> entry.distanceM?.takeIf { it > 0 }
        CardioMetric.DURATION -> entry.durationSec.toDouble()
        CardioMetric.HEART_RATE -> entry.avgHeartRate?.toDouble()
    }

    fun summary(entries: List<CardioEntry>): CardioSummary {
        val rates = entries.mapNotNull { it.avgHeartRate }
        return CardioSummary(
            sessions = entries.size,
            distanceM = entries.sumOf { it.distanceM ?: 0.0 },
            durationSec = entries.sumOf { it.durationSec.toLong() },
            avgHeartRate = if (rates.isEmpty()) null else rates.average().roundToInt(),
        )
    }
}
