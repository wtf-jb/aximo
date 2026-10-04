package io.github.wtfjb.aximo.domain.chat

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderFactory
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionApplier
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/**
 * Coach chat (B-05): questions about the user's own training. Each request
 * sends the aggregated context once and the shortened conversation. Plan
 * changes in the answer are stored as suggestions; applying them goes through
 * [io.github.wtfjb.aximo.domain.review.ReviewService.apply] after the user's tap.
 */
class ChatService(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    private val profiles: AiProfileRepository,
    private val factory: AiProviderFactory,
    private val generator: ChatGenerator,
    private val chats: AiChatRepository,
    private val time: TimeSource,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {

    fun observeMessages(): Flow<List<ChatMessage>> = chats.observeMessages()

    suspend fun context(): ChatContext = ChatContextBuilder.build(
        workouts = workouts.observeFinished().first(),
        routines = routines.observeRoutinesWithExercises().first(),
        exercises = exercises.observeExercises(includeArchived = true).first(),
        weeklyGoal = settings.training.first().weeklyGoal,
        today = StatsCalendar.localDate(time.now(), zone()),
        zone = zone(),
    )

    /**
     * Exactly what [send] would send for [question] now. A blank question
     * gives only the context and the history ("Gesendete Daten ansehen").
     */
    suspend fun payload(question: String): ChatPayload {
        val text = question.trim().take(ChatHistory.MAX_QUESTION_CHARS)
        val history = chats.observeMessages().first()
        val messages = if (text.isEmpty()) {
            ChatHistory.trim(history)
        } else {
            ChatHistory.trim(history, maxChars = ChatHistory.MAX_CHARS - text.length) +
                ChatMessage(role = ChatRole.USER, text = text, createdAt = time.now())
        }
        return ChatPayload(context(), messages)
    }

    /**
     * Sends [question] with the active profile and stores question and answer.
     * Suggestions that do not fit the current routines are dropped. Throws
     * [ChatException] or [AiException]; on failure nothing is stored.
     */
    suspend fun send(question: String, language: String) {
        if (question.isBlank()) throw ChatException(ChatException.Reason.EMPTY)
        val profile = AiProfiles.active(profiles.observeProfiles().first())
            ?: throw ChatException(ChatException.Reason.NO_PROFILE)
        val payload = payload(question)
        val provider = factory.create(profile, profiles.apiKey(profile.id))
        val generated = generator.generate(provider, payload, language)

        val valid = SuggestionApplier.applicable(
            generated.suggestions,
            routines.observeRoutinesWithExercises().first(),
            exercises.observeExercises(includeArchived = true).first(),
        )
        val answer = ChatMessage(
            role = ChatRole.COACH,
            text = generated.reply,
            createdAt = time.now(),
            suggestions = valid.map { AiSuggestion(change = it.change, rationale = it.rationale) },
            droppedSuggestions = generated.dropped + (generated.suggestions.size - valid.size),
        )
        chats.saveExchange(payload.messages.last(), answer)
    }

    /** "Neues Gespräch": deletes the conversation. Applied changes stay in the routines. */
    suspend fun clear() = chats.clear()
}
