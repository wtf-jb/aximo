package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.ai.SuggestionCodec
import io.github.wtfjb.aximo.data.db.dao.AiReviewDao
import io.github.wtfjb.aximo.data.db.entity.AiReviewEntity
import io.github.wtfjb.aximo.data.db.entity.AiSuggestionEntity
import io.github.wtfjb.aximo.domain.review.AiReview
import io.github.wtfjb.aximo.domain.review.AiReviewRepository
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAiReviewRepository(private val dao: AiReviewDao) : AiReviewRepository {

    override fun observeLatest(): Flow<AiReview?> = dao.observeLatest().map { row ->
        row?.let {
            AiReview(
                id = it.review.id,
                createdAt = it.review.createdAt,
                weeks = it.review.weeks,
                summary = it.review.summary,
                suggestions = it.suggestions.sortedBy { s -> s.position }.mapNotNull { s -> s.toDomain() },
                droppedSuggestions = it.review.droppedSuggestions,
            )
        }
    }

    override suspend fun saveReview(review: AiReview): Long = dao.save(
        AiReviewEntity(createdAt = review.createdAt, weeks = review.weeks, summary = review.summary, droppedSuggestions = review.droppedSuggestions),
        review.suggestions.mapIndexed { index, s ->
            AiSuggestionEntity(
                reviewId = null,
                position = index,
                type = SuggestionCodec.type(s.change),
                payloadJson = SuggestionCodec.encode(s.change),
                rationale = s.rationale,
                status = s.status.name,
            )
        },
    )

    override suspend fun getSuggestion(id: Long): AiSuggestion? = dao.getSuggestion(id)?.toDomain()

    override suspend fun setStatus(suggestionId: Long, status: SuggestionStatus) = dao.setStatus(suggestionId, status.name)

}

/** Shared with [RoomAiChatRepository]: both read rows of `ai_suggestions`. Null if the row cannot be read. */
internal fun AiSuggestionEntity.toDomain(): AiSuggestion? {
    val change = SuggestionCodec.decode(type, payloadJson) ?: return null
    val status = SuggestionStatus.entries.firstOrNull { it.name == status } ?: return null
    return AiSuggestion(id = id, reviewId = reviewId, chatMessageId = chatMessageId, change = change, rationale = rationale, status = status)
}
