package io.github.wtfjb.aximo.domain.rest

import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** What comes after the rest: "Satz 3 · Bankdrücken". [setNumber] 0 means a warm-up set. */
data class NextSet(val exerciseName: String, val setNumber: Int)

/**
 * A running rest between sets (A-03). Immutable: "+15 s" returns a new timer.
 * [next] is null when every set of the workout is done.
 */
data class RestTimer(
    val startedAt: Instant,
    val endsAt: Instant,
    val next: NextSet?,
) {
    init {
        require(endsAt >= startedAt) { "endsAt must not be before startedAt" }
    }

    /** Full length in seconds, including any extensions. */
    val totalSeconds: Long get() = (endsAt - startedAt).inWholeSeconds

    /** Seconds left, rounded up, so the display shows 0:01 until the very end. */
    fun remainingSeconds(now: Instant): Long {
        val millis = (endsAt - now).inWholeMilliseconds
        return if (millis <= 0) 0 else ceil(millis / 1000.0).toLong()
    }

    /** Share of the rest still left: 1 at the start, 0 at the end. */
    fun remainingFraction(now: Instant): Float {
        val total = (endsAt - startedAt).inWholeMilliseconds
        if (total <= 0) return 0f
        return ((endsAt - now).inWholeMilliseconds.toFloat() / total).coerceIn(0f, 1f)
    }

    fun isOver(now: Instant): Boolean = now >= endsAt

    fun extendedBy(seconds: Int): RestTimer = copy(endsAt = endsAt + seconds.seconds)
}
