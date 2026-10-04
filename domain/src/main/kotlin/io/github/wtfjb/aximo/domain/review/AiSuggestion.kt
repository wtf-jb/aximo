package io.github.wtfjb.aximo.domain.review

import kotlin.time.Instant

/**
 * One change the AI proposes to a routine (B-02). Exercises are addressed by
 * routine and exercise id: routine exercise ids change on every save.
 * The `from` values say what the AI saw; if the routine changed since, the
 * suggestion no longer applies.
 */
sealed interface SuggestionChange {
    val routineId: Long
    val exerciseId: Long

    data class SetCount(override val routineId: Long, override val exerciseId: Long, val from: Int, val to: Int) : SuggestionChange

    data class RepRange(
        override val routineId: Long,
        override val exerciseId: Long,
        val fromMin: Int,
        val fromMax: Int,
        val toMin: Int,
        val toMax: Int,
    ) : SuggestionChange

    data class TargetRir(override val routineId: Long, override val exerciseId: Long, val from: Int?, val to: Int?) : SuggestionChange

    data class AddExercise(
        override val routineId: Long,
        override val exerciseId: Long,
        val sets: Int,
        val repMin: Int,
        val repMax: Int,
        val targetRir: Int?,
    ) : SuggestionChange

    data class RemoveExercise(override val routineId: Long, override val exerciseId: Long) : SuggestionChange
}

enum class SuggestionStatus { OPEN, APPLIED, DISCARDED }

/** A stored suggestion. The AI never changes data itself; only [SuggestionApplier] does, after the user confirms. */
data class AiSuggestion(
    val id: Long = 0,
    val reviewId: Long,
    val change: SuggestionChange,
    /** Why, in the app language, naming the numbers ("9 Sätze, Ziel 10–20"). */
    val rationale: String,
    val status: SuggestionStatus = SuggestionStatus.OPEN,
)

/** One weekly review: summary text plus its suggestions. */
data class AiReview(
    val id: Long = 0,
    val createdAt: Instant,
    val weeks: Int,
    val summary: String,
    val suggestions: List<AiSuggestion> = emptyList(),
    /** Suggestions the AI sent that did not pass validation and were dropped. */
    val droppedSuggestions: Int = 0,
)
