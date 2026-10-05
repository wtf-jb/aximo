package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.ai.SuggestionCodec
import io.github.wtfjb.aximo.data.db.dao.AiChatDao
import io.github.wtfjb.aximo.data.db.entity.AiChatMessageEntity
import io.github.wtfjb.aximo.data.db.entity.AiSuggestionEntity
import io.github.wtfjb.aximo.domain.chat.AiChatRepository
import io.github.wtfjb.aximo.domain.chat.ChatMessage
import io.github.wtfjb.aximo.domain.chat.ChatRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Coach chat (B-05). Suggestions go into `ai_suggestions` like the review's, so applying works the same way. */
class RoomAiChatRepository(private val dao: AiChatDao) : AiChatRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> = dao.observeMessages().map { rows ->
        rows.mapNotNull { row ->
            val role = ChatRole.entries.firstOrNull { it.name == row.message.role } ?: return@mapNotNull null
            ChatMessage(
                id = row.message.id,
                role = role,
                text = row.message.text,
                createdAt = row.message.createdAt,
                suggestions = row.suggestions.sortedBy { it.position }.mapNotNull { it.toDomain() },
                droppedSuggestions = row.message.droppedSuggestions,
            )
        }
    }

    override suspend fun saveExchange(question: ChatMessage, answer: ChatMessage) = dao.saveExchange(
        question.toEntity(),
        answer.toEntity(),
        answer.suggestions.mapIndexed { index, s ->
            AiSuggestionEntity(
                reviewId = null,
                position = index,
                type = SuggestionCodec.type(s.change),
                payloadJson = SuggestionCodec.encode(s.change),
                rationale = s.rationale,
                status = s.status.name,
                reason = s.reason?.name,
            )
        },
    )

    override suspend fun clear() = dao.clear()

    private fun ChatMessage.toEntity() = AiChatMessageEntity(
        role = role.name,
        text = text,
        createdAt = createdAt,
        droppedSuggestions = droppedSuggestions,
    )
}
