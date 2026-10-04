package io.github.wtfjb.aximo.domain.workout

import io.github.wtfjb.aximo.domain.model.SetEntry
import kotlin.math.floor
import kotlin.math.roundToInt

/** Which scale rates how hard a set was (A-02): reps in reserve or rate of perceived exertion. */
enum class SetRating { RIR, RPE }

/**
 * RIR and RPE describe the same thing: RPE 10 = no rep left, RPE 8 = two left.
 * A set stores the scale it was logged in; the other one is derived for display
 * and for the progression rules.
 */
object Effort {
    const val MIN_RPE = 1.0
    const val MAX_RPE = 10.0

    /** RIR of a set: logged RIR, else derived from RPE (RPE 8.5 → 1), else null. */
    fun rir(set: SetEntry): Int? = set.rpe?.let { rirFromRpe(it) } ?: set.rir

    /** RPE of a set: logged RPE, else derived from RIR (RIR 2 → 8, never below 1), else null. */
    fun rpe(set: SetEntry): Double? = set.rpe ?: set.rir?.let { rpeFromRir(it) }

    fun rirFromRpe(rpe: Double): Int = floor(MAX_RPE - rpe).toInt().coerceAtLeast(0)

    fun rpeFromRir(rir: Int): Double = (MAX_RPE - rir).coerceAtLeast(MIN_RPE)

    /** Typed RPE ("8", "8,5"), rounded to half points; null if not a number from 1 to 10. */
    fun parseRpe(text: String): Double? {
        val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (value < MIN_RPE || value > MAX_RPE) return null
        return (value * 2).roundToInt() / 2.0
    }

    /** A set with a new RIR; the RPE is cleared, so only one scale is stored. */
    fun withRir(set: SetEntry, rir: Int?): SetEntry = set.copy(rir = rir, rpe = null)

    /** A set with a new RPE; the RIR is cleared, so only one scale is stored. */
    fun withRpe(set: SetEntry, rpe: Double?): SetEntry = set.copy(rpe = rpe, rir = null)
}
