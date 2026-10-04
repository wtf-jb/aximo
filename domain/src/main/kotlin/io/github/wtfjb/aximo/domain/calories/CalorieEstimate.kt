package io.github.wtfjb.aximo.domain.calories

import io.github.wtfjb.aximo.domain.cardio.CardioMath
import io.github.wtfjb.aximo.domain.cardio.DefaultActivity
import kotlin.math.min

/**
 * Conservative estimate of the calories a session burns on top of what the body
 * burns at rest anyway ("net" kcal). Deliberately low: fitness watches usually show
 * gross values that include the resting metabolism.
 *
 * Time-based activities use METs from the Compendium of Physical Activities, at the
 * lower end of each range: net kcal = (MET − 1) × body weight (kg) × hours.
 * Running and walking with a distance use the energy cost per kilometre, which
 * barely depends on the speed. Elevation and heart rate are ignored on purpose.
 *
 * All results are rounded down to whole 10 kcal.
 */
object CalorieEstimate {

    /** Strength training over the whole workout including rests (MET 3.5). */
    fun strength(durationSec: Long, bodyWeightKg: Double): Int {
        // A workout left running by mistake should not count for hours.
        val seconds = min(durationSec, MAX_STRENGTH_SECONDS).coerceAtLeast(0)
        return netKcal(STRENGTH_MET, seconds, bodyWeightKg)
    }

    /**
     * A cardio session. [activity] is null for the user's own activities, which
     * count as moderate general activity.
     */
    fun cardio(activity: DefaultActivity?, durationSec: Int, distanceM: Double?, bodyWeightKg: Double): Int {
        val seconds = durationSec.toLong().coerceAtLeast(0)
        val distance = distanceM?.takeIf { it > 0 }
        return when (activity) {
            DefaultActivity.RUNNING -> if (distance == null) {
                netKcal(RUNNING_MET, seconds, bodyWeightKg)
            } else {
                running(seconds, distance, bodyWeightKg)
            }
            DefaultActivity.CYCLING -> netKcal(cyclingMet(seconds, distance), seconds, bodyWeightKg)
            DefaultActivity.ROWING -> netKcal(rowingMet(seconds, distance), seconds, bodyWeightKg)
            DefaultActivity.OTHER, null -> netKcal(OTHER_MET, seconds, bodyWeightKg)
        }
    }

    /** Per kilometre; below [WALKING_MAX_KMH] it is walking, which costs about half. */
    private fun running(seconds: Long, distanceM: Double, bodyWeightKg: Double): Int {
        val speed = CardioMath.speedKmh(seconds.toInt(), distanceM) ?: 0.0
        val perKgKm = if (speed < WALKING_MAX_KMH) WALKING_KCAL_PER_KG_KM else RUNNING_KCAL_PER_KG_KM
        return roundDown(perKgKm * bodyWeightKg * distanceM / METERS_PER_KM)
    }

    /** By average speed; without a distance (e.g. indoor bike) a moderate value. */
    private fun cyclingMet(seconds: Long, distanceM: Double?): Double {
        val speed = distanceM?.let { CardioMath.speedKmh(seconds.toInt(), it) } ?: return CYCLING_DEFAULT_MET
        return when {
            speed < 16 -> 4.0
            speed < 19 -> 6.0
            speed < 22.5 -> 7.5
            else -> 9.0
        }
    }

    /** By pace per 500 m on the rowing machine; without a distance a moderate value. */
    private fun rowingMet(seconds: Long, distanceM: Double?): Double {
        val pace = distanceM?.let { CardioMath.paceSecondsPer500m(seconds.toInt(), it) } ?: return ROWING_DEFAULT_MET
        return when {
            pace > 150 -> 4.8
            pace > 125 -> 7.0
            else -> 8.5
        }
    }

    private fun netKcal(met: Double, seconds: Long, bodyWeightKg: Double): Int =
        roundDown((met - 1) * bodyWeightKg * seconds / SECONDS_PER_HOUR)

    private fun roundDown(kcal: Double): Int = (kcal.coerceAtLeast(0.0) / 10).toInt() * 10

    private const val STRENGTH_MET = 3.5
    private const val RUNNING_MET = 7.0
    private const val CYCLING_DEFAULT_MET = 5.0
    private const val ROWING_DEFAULT_MET = 6.0
    private const val OTHER_MET = 4.0

    /** Net cost of running and walking on flat ground, kcal per kg body weight and km. */
    private const val RUNNING_KCAL_PER_KG_KM = 0.9
    private const val WALKING_KCAL_PER_KG_KM = 0.5
    private const val WALKING_MAX_KMH = 6.5

    private const val MAX_STRENGTH_SECONDS = 3 * 3600L
    private const val SECONDS_PER_HOUR = 3600.0
    private const val METERS_PER_KM = 1000.0
}
