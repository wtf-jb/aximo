package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.chat.ChatContext
import io.github.wtfjb.aximo.domain.chat.ChatMessage
import io.github.wtfjb.aximo.domain.chat.ChatPayload
import io.github.wtfjb.aximo.domain.chat.ChatRole
import io.github.wtfjb.aximo.domain.chat.ExerciseHistory
import io.github.wtfjb.aximo.domain.chat.HistoryBlock
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.EffortTrend
import io.github.wtfjb.aximo.domain.review.RegionVolume
import io.github.wtfjb.aximo.domain.review.ReviewContext
import io.github.wtfjb.aximo.domain.review.RoutineExerciseInfo
import io.github.wtfjb.aximo.domain.review.RoutineInfo
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionReason
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class ChatPromptTest {

    private val context = ChatContext(
        review = ReviewContext(
            today = LocalDate(2026, 10, 4),
            weeks = 6,
            sessions = 4,
            sessionsPerWeek = 0.7,
            weeklyGoal = 3,
            routines = listOf(RoutineInfo(7, "Push A", 4, listOf(RoutineExerciseInfo(1, "Bankdrücken", 3, 6, 8, 2)))),
            exercises = emptyList(),
            volume = listOf(RegionVolume(BodyRegion.CHEST, 1.3, 10.0, 20.0)),
            effort = EffortTrend(null, null),
        ),
        history = listOf(
            ExerciseHistory(
                1, "Bankdrücken", ProgressMetric.E1RM,
                listOf(HistoryBlock(LocalDate(2026, 4, 20), 2, 101.3), HistoryBlock(LocalDate(2026, 5, 18), 0, null)),
            ),
        ),
    )

    private val at = Instant.fromEpochSeconds(0)
    private val payload = ChatPayload(
        context,
        listOf(
            ChatMessage(role = ChatRole.USER, text = "Warum stagniert mein Bankdrücken?", createdAt = at),
            ChatMessage(
                role = ChatRole.COACH,
                text = "Seit 3 Einheiten kein Bestwert.",
                createdAt = at,
                suggestions = listOf(
                    AiSuggestion(change = SuggestionChange.RepRange(7, 1, 6, 8, 8, 10), rationale = "mehr Wdh.", reason = SuggestionReason.STAGNATION),
                ),
            ),
            ChatMessage(role = ChatRole.USER, text = "Und jetzt?", createdAt = at),
        ),
    )

    @Test
    fun contextGoesOnceIntoTheSystemPrompt() {
        val request = ChatPrompt.request(payload, "de")

        assertTrue(request.system!!.contains("in German"))
        assertTrue(request.system!!.contains("\"prompt_version\":\"${ChatPrompt.VERSION}\""))
        assertTrue(request.system!!.contains("\"routine_id\":7"))
        assertEquals(listOf(AiMessage.Role.USER, AiMessage.Role.ASSISTANT, AiMessage.Role.USER), request.messages.map { it.role })
        assertFalse(request.messages.any { it.text.contains("prompt_version") })
        assertEquals("Und jetzt?", request.messages.last().text)
    }

    @Test
    fun earlierAnswersAreSentInTheReplyFormat() {
        val answer = Json.parseToJsonElement(ChatPrompt.message(payload.messages[1]).text).jsonObject

        assertEquals("Seit 3 Einheiten kein Bestwert.", answer["reply"]!!.jsonPrimitive.content)
        val suggestion = answer["suggestions"]!!.jsonArray.single().jsonObject
        assertEquals("rep_range", suggestion["type"]!!.jsonPrimitive.content)
        assertEquals("stagnation", suggestion["reason"]!!.jsonPrimitive.content)
        assertEquals(SuggestionReason.STAGNATION, ChatParser.parse(answer.toString()).suggestions.single().reason)
        // Round trip through the parser gives the same change.
        assertEquals(SuggestionChange.RepRange(7, 1, 6, 8, 8, 10), ChatParser.parse(answer.toString()).suggestions.single().change)
    }

    @Test
    fun versionAndStatusRules() {
        assertEquals("chat-v3", ChatPrompt.VERSION)
        val system = ChatPrompt.system("en")
        listOf("regressing", "returning", "sessions_at_rep_ceiling", "\"reason\"", "\"reason\": string").forEach { assertTrue(it, system.contains(it)) }
    }

    @Test
    fun contextJsonHasReviewDataAndLongTermHistory() {
        val json = ChatPrompt.contextJson(context)

        assertEquals(ChatPrompt.VERSION, json["prompt_version"]!!.jsonPrimitive.content)
        assertEquals(3, json["weekly_goal"]!!.jsonPrimitive.content.toInt())
        val history = json["long_term_history"]!!.jsonArray.single().jsonObject
        assertEquals("e1rm_kg", history["metric"]!!.jsonPrimitive.content)
        val blocks = history["blocks"]!!.jsonArray
        assertEquals("2026-04-20", blocks[0].jsonObject["start"]!!.jsonPrimitive.content)
        assertEquals(101.3, blocks[0].jsonObject["best"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals(JsonNull, blocks[1].jsonObject["best"])
    }

    @Test
    fun prettyPayloadShowsContextAndMessagesAsSent() {
        val shown = Json.parseToJsonElement(ChatPrompt.prettyPayload(payload)).jsonObject

        assertEquals(ChatPrompt.contextJson(context), shown["context"])
        val messages = shown["messages"]!!.jsonArray
        assertEquals(3, messages.size)
        assertEquals("assistant", messages[1].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals(ChatPrompt.message(payload.messages[1]).text, messages[1].jsonObject["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun conversationGoesOverTheWire() = runBlocking {
        val server = MockServer {
            json("""{"choices":[{"index":0,"message":{"role":"assistant","content":"{\"reply\":\"Mehr Schlaf.\",\"suggestions\":[]}"}}]}""")
        }
        val provider = OpenAiCompatibleProvider(server.client, "https://api.example.com/v1", "gpt-test", "k")

        val reply = KtorChatGenerator().generate(provider, payload, "en")

        assertEquals("Mehr Schlaf.", reply.reply)
        val messages = server.lastRequest.jsonBody()["messages"]!!.jsonArray.map { it.jsonObject["role"]!!.jsonPrimitive.content }
        assertEquals(listOf("system", "user", "assistant", "user"), messages)
    }
}
