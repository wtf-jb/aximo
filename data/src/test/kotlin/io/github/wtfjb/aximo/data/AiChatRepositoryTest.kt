package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.repository.RoomAiChatRepository
import io.github.wtfjb.aximo.data.repository.RoomAiReviewRepository
import io.github.wtfjb.aximo.domain.chat.ChatMessage
import io.github.wtfjb.aximo.domain.chat.ChatRole
import io.github.wtfjb.aximo.domain.review.AiReview
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.PlanEntry
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.PlanRoutine
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionReason
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class AiChatRepositoryTest : DatabaseTest() {

    private lateinit var chats: RoomAiChatRepository
    private lateinit var reviews: RoomAiReviewRepository

    @Before
    fun setUp() {
        chats = RoomAiChatRepository(db.aiChatDao())
        reviews = RoomAiReviewRepository(db.aiReviewDao())
    }

    private fun question(at: Long, text: String) = ChatMessage(role = ChatRole.USER, text = text, createdAt = Instant.fromEpochSeconds(at))

    private fun answer(at: Long, text: String, vararg changes: SuggestionChange) = ChatMessage(
        role = ChatRole.COACH,
        text = text,
        createdAt = Instant.fromEpochSeconds(at),
        suggestions = changes.map { AiSuggestion(change = it, rationale = "weil") },
        droppedSuggestions = 1,
    )

    @Test
    fun exchangesAreStoredInOrderWithSuggestions() = runTest {
        assertTrue(chats.observeMessages().first().isEmpty())

        chats.saveExchange(question(1, "Warum?"), answer(2, "Darum.", SuggestionChange.SetCount(1, 2, 3, 4)))
        chats.saveExchange(question(3, "Und?"), answer(4, "Nichts."))

        val messages = chats.observeMessages().first()
        assertEquals(listOf("Warum?", "Darum.", "Und?", "Nichts."), messages.map { it.text })
        assertEquals(listOf(ChatRole.USER, ChatRole.COACH, ChatRole.USER, ChatRole.COACH), messages.map { it.role })
        val suggestion = messages[1].suggestions.single()
        assertEquals(SuggestionChange.SetCount(1, 2, 3, 4), suggestion.change)
        assertEquals(messages[1].id, suggestion.chatMessageId)
        assertNull(suggestion.reviewId)
        assertEquals(1, messages[1].droppedSuggestions)
    }

    @Test
    fun reasonOfChatSuggestionsIsStored() = runTest {
        val withReason = answer(2, "A", SuggestionChange.SetCount(1, 2, 3, 4))
            .let { it.copy(suggestions = it.suggestions.map { s -> s.copy(reason = SuggestionReason.EFFORT_LOW) }) }
        chats.saveExchange(question(1, "Q"), withReason)

        assertEquals(SuggestionReason.EFFORT_LOW, chats.observeMessages().first()[1].suggestions.single().reason)
    }

    @Test
    fun chatSuggestionsUseTheReviewConfirmFlow() = runTest {
        chats.saveExchange(question(1, "Mehr Sätze?"), answer(2, "Ja.", SuggestionChange.SetCount(1, 2, 3, 4)))
        val id = chats.observeMessages().first()[1].suggestions.single().id

        assertNotNull(reviews.getSuggestion(id))
        reviews.setStatus(id, SuggestionStatus.APPLIED)

        assertEquals(SuggestionStatus.APPLIED, chats.observeMessages().first()[1].suggestions.single().status)
        // The review does not see chat suggestions.
        assertNull(reviews.observeLatest().first())
    }

    @Test
    fun clearDeletesOnlyTheChat() = runTest {
        reviews.saveReview(
            AiReview(
                createdAt = Instant.fromEpochSeconds(1),
                weeks = 6,
                summary = "s",
                suggestions = listOf(AiSuggestion(change = SuggestionChange.SetCount(1, 2, 3, 4), rationale = "r")),
            ),
        )
        chats.saveExchange(question(1, "Q"), answer(2, "A", SuggestionChange.SetCount(1, 2, 3, 5)))
        val chatSuggestion = chats.observeMessages().first()[1].suggestions.single().id

        chats.clear()

        assertTrue(chats.observeMessages().first().isEmpty())
        assertNull(reviews.getSuggestion(chatSuggestion))
        assertEquals(1, reviews.observeLatest().first()!!.suggestions.size)
    }

    @Test
    fun newChangeTypesAreStored() = runTest {
        val changes = listOf(
            SuggestionChange.ReplaceExercise(1, 2, 3),
            SuggestionChange.MoveExercise(1, 2, 0, 1),
            SuggestionChange.RenameRoutine(1, "Push A", "Oberkörper A"),
            SuggestionChange.DeleteRoutine(1, "Push A"),
            SuggestionChange.CreatePlan(
                listOf(
                    PlanRoutine(
                        "Ganzkörper A",
                        listOf(
                            PlanEntry(PlanExerciseRef.Existing(2), 3, 8, 12, 2),
                            PlanEntry(PlanExerciseRef.New("Kabelzug", ExerciseType.STRENGTH, Equipment.CABLE, "fed_Cable_Crossover", "Cable Crossover"), 3, 10, 15, null),
                        ),
                    ),
                ),
            ),
        )

        chats.saveExchange(question(1, "Q"), answer(2, "A", *changes.toTypedArray()))

        assertEquals(changes, chats.observeMessages().first()[1].suggestions.map { it.change })
    }
}
