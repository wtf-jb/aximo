package io.github.wtfjb.aximo.domain.chat

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.ReviewContext
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Who wrote a chat message (B-05). */
enum class ChatRole { USER, COACH }

/**
 * One message of the coach chat. A coach message can carry suggestions; like
 * the review's, they change nothing until the user taps "Übernehmen".
 */
data class ChatMessage(
    val id: Long = 0,
    val role: ChatRole,
    val text: String,
    val createdAt: Instant,
    val suggestions: List<AiSuggestion> = emptyList(),
    /** Suggestions the AI sent that did not pass validation and were dropped. */
    val droppedSuggestions: Int = 0,
)

/**
 * What the chat sends besides the conversation: the weekly review's aggregated
 * data plus a longer, coarse history per exercise. No single sets, notes,
 * cardio or health data. Weights in kg.
 */
data class ChatContext(
    val review: ReviewContext,
    val history: List<ExerciseHistory>,
)

/** Best value of an exercise per block of weeks, oldest block first. */
data class ExerciseHistory(
    val exerciseId: Long,
    val name: String,
    /** E1RM (kg) for weighted exercises, most reps for bodyweight. */
    val metric: ProgressMetric,
    val blocks: List<HistoryBlock>,
)

/** [best] is null if the exercise was not trained in the block. */
data class HistoryBlock(val start: LocalDate, val sessions: Int, val best: Double?)

/** Exactly what one chat request sends: the context once, then the (shortened) conversation ending with the question. */
data class ChatPayload(val context: ChatContext, val messages: List<ChatMessage>)

/** The AI's answer, parsed and checked against the schema. */
data class GeneratedReply(
    val reply: String,
    val suggestions: List<GeneratedSuggestion> = emptyList(),
    /** Entries that did not match the schema. */
    val dropped: Int = 0,
)

/** Builds the prompt, calls the provider and parses the answer; lives in `:ai`. */
fun interface ChatGenerator {
    /** [language] is the app language as a tag ("de", "en"); the reply is written in it. */
    suspend fun generate(provider: AiProvider, payload: ChatPayload, language: String): GeneratedReply
}

/** Why a chat message could not be sent, besides [io.github.wtfjb.aximo.domain.ai.AiException]. */
class ChatException(val reason: Reason) : Exception("Chat not possible: $reason") {
    enum class Reason { NO_PROFILE, EMPTY }
}

/** The stored conversation (one at a time). Implemented in :data, not part of the JSON backup. */
interface AiChatRepository {
    /** All messages, oldest first, with their suggestions. */
    fun observeMessages(): Flow<List<ChatMessage>>

    /** Saves the question and the answer with its suggestions, all or nothing. */
    suspend fun saveExchange(question: ChatMessage, answer: ChatMessage)

    /** Deletes the conversation and its suggestions ("Neues Gespräch"). */
    suspend fun clear()
}
