package io.github.wtfjb.aximo.domain.stats

import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType

/** Kind of personal record. */
enum class RecordType { E1RM, WEIGHT, REPS_AT_WEIGHT, VOLUME }

/** A new best value. [weightKg] is the weight for REPS_AT_WEIGHT, else unused. */
data class PersonalRecord(
    val type: RecordType,
    val value: Double,
    val previous: Double,
    val weightKg: Double = 0.0,
)

/** e1RM, volume and personal records (A-07). Warm-ups never count. */
object Records {

    /** Epley only up to this many reps; above that the estimate is too rough. */
    const val E1RM_MAX_REPS = 12

    /** Estimated one-rep max (Epley): weight × (1 + reps / 30); one rep is the weight itself. Null above 12 reps. */
    fun e1rm(weightKg: Double, reps: Int): Double? = when {
        reps <= 0 || reps > E1RM_MAX_REPS -> null
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }

    /** Completed sets that count for records and volume: everything except warm-ups. */
    fun countingSets(sets: List<SetEntry>): List<SetEntry> =
        sets.filter { it.completedAt != null && it.setType != SetType.WARM_UP }

    /** Volume: sum of weight × reps over the counting sets. */
    fun volume(sets: List<SetEntry>): Double = countingSets(sets).sumOf { it.weightKg * it.reps }

    fun bestE1rm(sets: List<SetEntry>): Double? = countingSets(sets).mapNotNull { e1rm(it.weightKg, it.reps) }.maxOrNull()

    /**
     * The most notable new record of an exercise in this session compared to all
     * earlier sessions ([history]), in the order e1RM, weight, reps at weight, volume.
     * Null for the first session (nothing to compare with) or without a record.
     */
    fun newRecord(current: List<SetEntry>, history: List<SetEntry>): PersonalRecord? {
        val now = countingSets(current)
        val before = countingSets(history)
        if (now.isEmpty() || before.isEmpty()) return null

        val e1rmNow = bestE1rm(now)
        val e1rmBefore = bestE1rm(before)
        if (e1rmNow != null && e1rmBefore != null && e1rmNow > e1rmBefore + EPSILON) {
            return PersonalRecord(RecordType.E1RM, e1rmNow, e1rmBefore)
        }

        val weightNow = now.maxOf { it.weightKg }
        val weightBefore = before.maxOf { it.weightKg }
        if (weightNow > weightBefore + EPSILON) return PersonalRecord(RecordType.WEIGHT, weightNow, weightBefore)

        val repsNow = now.filter { it.weightKg >= weightNow - EPSILON }.maxOf { it.reps }
        val repsBefore = before.filter { it.weightKg >= weightNow - EPSILON }.maxOfOrNull { it.reps }
        if (repsBefore != null && repsNow > repsBefore) {
            return PersonalRecord(RecordType.REPS_AT_WEIGHT, repsNow.toDouble(), repsBefore.toDouble(), weightNow)
        }

        val volumeNow = volume(now)
        val volumeBefore = before.groupBy { it.workoutExerciseId }.values.maxOf { volume(it) }
        if (volumeNow > volumeBefore + EPSILON) return PersonalRecord(RecordType.VOLUME, volumeNow, volumeBefore)
        return null
    }

    private const val EPSILON = 1e-6
}
