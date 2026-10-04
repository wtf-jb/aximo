package io.github.wtfjb.aximo.domain.review

import kotlinx.coroutines.flow.Flow

/** Stored reviews and their suggestions (B-02). */
interface AiReviewRepository {
    /** The newest review with its suggestions, null before the first one. */
    fun observeLatest(): Flow<AiReview?>

    /** Saves the review and its suggestions; returns the review id. */
    suspend fun saveReview(review: AiReview): Long

    suspend fun getSuggestion(id: Long): AiSuggestion?

    suspend fun setStatus(suggestionId: Long, status: SuggestionStatus)
}
