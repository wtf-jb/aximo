package io.github.wtfjb.aximo.domain.cardio

import kotlin.math.roundToLong

/** Pace and speed from duration and distance (A-04). */
object CardioMath {

    /** Seconds per kilometre, or null without a positive distance. */
    fun paceSecondsPerKm(durationSec: Int, distanceM: Double?): Double? =
        if (distanceM == null || distanceM <= 0) null else durationSec / (distanceM / METERS_PER_KM)

    /** Seconds per 500 m (rowing), or null without a positive distance. */
    fun paceSecondsPer500m(durationSec: Int, distanceM: Double?): Double? =
        paceSecondsPerKm(durationSec, distanceM)?.div(2)

    /** Average speed in km/h, or null without a positive distance and duration. */
    fun speedKmh(durationSec: Int, distanceM: Double?): Double? =
        if (distanceM == null || distanceM <= 0 || durationSec <= 0) {
            null
        } else {
            (distanceM / METERS_PER_KM) / (durationSec / SECONDS_PER_HOUR)
        }

    /** Pace rounded to whole seconds: 331.4 → "5:31". */
    fun formatPace(seconds: Double): String = formatDuration(seconds.roundToLong())

    /** Duration as "28:41" or "1:05:30". */
    fun formatDuration(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Hours, minutes and seconds of a duration, for the entry fields. */
    fun split(totalSeconds: Int): Triple<Int, Int, Int> =
        Triple(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)

    /**
     * Duration from the three entry fields; a blank field counts as 0.
     * Null if a field is not a whole number ≥ 0 or the total is 0.
     */
    fun durationOf(hours: String, minutes: String, seconds: String): Int? {
        val parts = listOf(hours, minutes, seconds).map { text ->
            if (text.isBlank()) 0 else text.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return null
        }
        val total = parts[0] * 3600 + parts[1] * 60 + parts[2]
        return total.takeIf { it > 0 }
    }

    private const val METERS_PER_KM = 1000.0
    private const val SECONDS_PER_HOUR = 3600.0
}
