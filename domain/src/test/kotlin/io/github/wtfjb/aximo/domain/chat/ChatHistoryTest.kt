package io.github.wtfjb.aximo.domain.chat

import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class ChatHistoryTest {

    private fun msg(index: Int, text: String = "m$index") = ChatMessage(
        id = index.toLong(),
        role = if (index % 2 == 0) ChatRole.USER else ChatRole.COACH,
        text = text,
        createdAt = Instant.fromEpochSeconds(index.toLong()),
    )

    private val conversation = (0 until 20).map { msg(it) }

    @Test
    fun keepsTheLastMessages() {
        val trimmed = ChatHistory.trim(conversation, maxMessages = 4)

        assertEquals(listOf(16L, 17L, 18L, 19L), trimmed.map { it.id })
    }

    @Test
    fun startsWithAQuestion() {
        // 5 would start with an answer (id 15), so it is dropped.
        val trimmed = ChatHistory.trim(conversation, maxMessages = 5)

        assertEquals(listOf(16L, 17L, 18L, 19L), trimmed.map { it.id })
        assertEquals(ChatRole.USER, trimmed.first().role)
    }

    @Test
    fun respectsTheCharacterBudget() {
        val long = conversation.map { it.copy(text = "x".repeat(100)) }

        val trimmed = ChatHistory.trim(long, maxMessages = 20, maxChars = 350)

        // 3 × 100 chars fit; the oldest of them is an answer and goes.
        assertEquals(listOf(18L, 19L), trimmed.map { it.id })
    }

    @Test
    fun suggestionsCountTowardsTheBudget() {
        val answer = msg(1).copy(suggestions = listOf(AiSuggestion(change = SuggestionChange.SetCount(1, 1, 3, 4), rationale = "abc")))

        assertEquals(2 + 3 + 120, ChatHistory.size(answer))
    }

    @Test
    fun emptyStaysEmpty() {
        assertTrue(ChatHistory.trim(emptyList()).isEmpty())
        assertTrue(ChatHistory.trim(conversation, maxChars = 0).isEmpty())
    }
}
