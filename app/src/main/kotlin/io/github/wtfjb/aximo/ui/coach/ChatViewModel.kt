package io.github.wtfjb.aximo.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.chat.ChatException
import io.github.wtfjb.aximo.domain.chat.ChatHistory
import io.github.wtfjb.aximo.domain.chat.ChatMessage
import io.github.wtfjb.aximo.domain.chat.ChatPayload
import io.github.wtfjb.aximo.domain.chat.ChatService
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.review.ReviewException
import io.github.wtfjb.aximo.domain.review.ReviewService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A chat message with the cards of its suggestions. */
data class ChatEntry(val message: ChatMessage, val items: List<SuggestionItem>)

data class ChatUiState(
    val loading: Boolean = true,
    val entries: List<ChatEntry> = emptyList(),
    /** Text in the input field. */
    val draft: String = "",
    /** The question being sent; shown as a bubble while the coach thinks. */
    val pending: String? = null,
    val error: CoachError? = null,
    /** Chat notice before the first message. */
    val showNotice: Boolean = false,
    /** Pretty JSON of what is sent, while the dialog is open. */
    val sentData: String? = null,
    /** "Neues Gespräch" asks first. */
    val confirmClear: Boolean = false,
    /** Routines the user switched off, per plan suggestion id. */
    val excluded: Map<Long, Set<Int>> = emptyMap(),
) {
    val sending: Boolean get() = pending != null
    val canSend: Boolean get() = draft.isNotBlank() && !sending
    val isEmpty: Boolean get() = entries.isEmpty() && pending == null
}

private data class ChatTransient(
    val draft: String = "",
    val pending: String? = null,
    val error: CoachError? = null,
    val showNotice: Boolean = false,
    val sentData: String? = null,
    val confirmClear: Boolean = false,
    val excluded: Map<Long, Set<Int>> = emptyMap(),
)

/**
 * Coach chat (B-05). Suggestions in answers are applied only on the user's tap,
 * through the same [ReviewService.apply] as the review's.
 */
class ChatViewModel(
    private val chat: ChatService,
    private val review: ReviewService,
    routines: RoutineRepository,
    exercises: ExerciseRepository,
    private val preferences: AiPreferences,
    /** App language tag for the AI's answer, e.g. "de". */
    private val language: () -> String,
    /** Renders what is sent (pretty JSON). */
    private val formatPayload: (ChatPayload) -> String,
) : ViewModel() {

    private val transient = MutableStateFlow(ChatTransient())

    val uiState: StateFlow<ChatUiState> = combine(
        chat.observeMessages(),
        routines.observeRoutinesWithExercises(),
        exercises.observeExercises(includeArchived = true),
        transient,
    ) { messages, routineList, exerciseList, t ->
        ChatUiState(
            loading = false,
            entries = messages.map { m -> ChatEntry(m, m.suggestions.map { suggestionItem(it, routineList, exerciseList) }) },
            draft = t.draft,
            pending = t.pending,
            error = t.error,
            showNotice = t.showNotice,
            sentData = t.sentData,
            confirmClear = t.confirmClear,
            excluded = t.excluded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    fun setDraft(text: String) = transient.update { it.copy(draft = text.take(ChatHistory.MAX_QUESTION_CHARS)) }

    /** First message: the chat notice; afterwards straight to the AI. */
    fun send() {
        val t = transient.value
        if (t.draft.isBlank() || t.pending != null) return
        viewModelScope.launch {
            if (preferences.chatNoticeAccepted.first()) run() else transient.update { it.copy(showNotice = true) }
        }
    }

    fun acceptNotice() {
        transient.update { it.copy(showNotice = false) }
        viewModelScope.launch {
            preferences.acceptChatNotice()
            run()
        }
    }

    fun dismissNotice() = transient.update { it.copy(showNotice = false) }

    /** Context plus the conversation as the next message would send it, including the current draft. */
    fun showSentData() {
        viewModelScope.launch {
            val text = formatPayload(chat.payload(transient.value.draft))
            transient.update { it.copy(sentData = text) }
        }
    }

    fun dismissSentData() = transient.update { it.copy(sentData = null) }

    fun apply(suggestionId: Long) {
        viewModelScope.launch { review.apply(suggestionId) }
    }

    /** "N Routinen speichern" on a plan card: saves the routines that are still switched on. */
    fun applyPlan(suggestionId: Long) {
        val excluded = transient.value.excluded[suggestionId].orEmpty()
        viewModelScope.launch { chat.applyPlan(suggestionId, excluded) }
    }

    /** Switches one routine of a plan card on or off. */
    fun togglePlanRoutine(suggestionId: Long, index: Int) = transient.update { t ->
        val current = t.excluded[suggestionId].orEmpty()
        val next = if (index in current) current - index else current + index
        t.copy(excluded = t.excluded + (suggestionId to next))
    }

    fun discard(suggestionId: Long) {
        viewModelScope.launch { review.discard(suggestionId) }
    }

    fun requestClear() = transient.update { it.copy(confirmClear = true) }

    fun dismissClear() = transient.update { it.copy(confirmClear = false) }

    fun confirmClear() {
        transient.update { it.copy(confirmClear = false, error = null) }
        viewModelScope.launch { chat.clear() }
    }

    private suspend fun run() {
        val question = transient.value.draft.trim()
        if (question.isEmpty() || transient.value.pending != null) return
        transient.update { it.copy(pending = question, draft = "", error = null) }
        val error = try {
            chat.send(question, language())
            null
        } catch (e: AiException) {
            CoachError.Ai(e.reason, e.statusCode, e.detail)
        } catch (e: ChatException) {
            // EMPTY cannot happen here: the question was checked above.
            if (e.reason == ChatException.Reason.NO_PROFILE) CoachError.Review(ReviewException.Reason.NO_PROFILE) else null
        }
        transient.update { t ->
            // On failure the question goes back into the field, so it can be sent again.
            if (error == null) {
                t.copy(pending = null)
            } else {
                t.copy(pending = null, error = error, draft = t.draft.ifBlank { question })
            }
        }
    }
}
