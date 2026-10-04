package io.github.wtfjb.aximo.ui.coach

import io.github.wtfjb.aximo.domain.review.AiReview
import io.github.wtfjb.aximo.domain.review.AiReviewRepository
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAiReviewRepository : AiReviewRepository {
    val latest = MutableStateFlow<AiReview?>(null)
    private var nextId = 1L

    override fun observeLatest(): Flow<AiReview?> = latest

    override suspend fun saveReview(review: AiReview): Long {
        val id = nextId++
        latest.value = review.copy(
            id = id,
            suggestions = review.suggestions.map { it.copy(id = nextId++, reviewId = id) },
        )
        return id
    }

    override suspend fun getSuggestion(id: Long): AiSuggestion? = latest.value?.suggestions?.firstOrNull { it.id == id }

    override suspend fun setStatus(suggestionId: Long, status: SuggestionStatus) {
        latest.value = latest.value?.let { r ->
            r.copy(suggestions = r.suggestions.map { if (it.id == suggestionId) it.copy(status = status) else it })
        }
    }
}
