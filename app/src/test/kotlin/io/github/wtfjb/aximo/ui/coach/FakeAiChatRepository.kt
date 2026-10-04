package io.github.wtfjb.aximo.ui.coach

import io.github.wtfjb.aximo.domain.chat.AiChatRepository
import io.github.wtfjb.aximo.domain.chat.ChatMessage
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAiChatRepository : AiChatRepository {
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    // Separate from FakeAiReviewRepository's ids, as rows of one table would be.
    private var nextId = 1_000L

    override fun observeMessages(): Flow<List<ChatMessage>> = messages

    override suspend fun saveExchange(question: ChatMessage, answer: ChatMessage) {
        val q = question.copy(id = nextId++)
        val answerId = nextId++
        val a = answer.copy(id = answerId, suggestions = answer.suggestions.map { it.copy(id = nextId++, chatMessageId = answerId) })
        messages.value = messages.value + q + a
    }

    override suspend fun clear() {
        messages.value = emptyList()
    }

    fun findSuggestion(id: Long): AiSuggestion? = messages.value.flatMap { it.suggestions }.firstOrNull { it.id == id }

    fun setStatus(id: Long, status: SuggestionStatus) {
        messages.value = messages.value.map { m ->
            m.copy(suggestions = m.suggestions.map { if (it.id == id) it.copy(status = status) else it })
        }
    }
}
