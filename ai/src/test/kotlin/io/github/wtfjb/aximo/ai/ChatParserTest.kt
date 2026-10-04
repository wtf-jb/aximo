package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChatParserTest {

    private fun invalid(text: String) {
        try {
            ChatParser.parse(text)
            fail("Expected INVALID_RESPONSE for $text")
        } catch (e: AiException) {
            assertEquals(AiException.Reason.INVALID_RESPONSE, e.reason)
        }
    }

    @Test
    fun replyWithoutSuggestionsIsNormal() {
        val reply = ChatParser.parse("""{"reply":" Drei Einheiten ohne Bestwert. "}""")

        assertEquals("Drei Einheiten ohne Bestwert.", reply.reply)
        assertTrue(reply.suggestions.isEmpty())
        assertEquals(0, reply.dropped)
    }

    @Test
    fun keepsValidSuggestionsAndCountsTheRest() {
        val reply = ChatParser.parse(
            """
            ```json
            {"reply":"r","suggestions":[
              {"type":"set_count","routine_id":7,"exercise_id":1,"from":3,"to":4,"rationale":"9 Sätze"},
              {"type":"set_count","routine_id":"7","exercise_id":1,"from":3,"to":4,"rationale":"id as string"},
              {"type":"rename_routine","routine_id":7,"exercise_id":1,"rationale":"unknown type"},
              {"type":"remove_exercise","routine_id":7,"exercise_id":2,"rationale":"x"},
              {"type":"remove_exercise","routine_id":7,"exercise_id":3,"rationale":"over the limit"}
            ]}
            ```
            """.trimIndent(),
        )

        assertEquals(SuggestionChange.SetCount(7, 1, 3, 4), reply.suggestions.single().change)
        assertEquals(4, reply.dropped)
    }

    @Test
    fun plainTextIsAnAnswerWithoutSuggestions() {
        assertEquals("Mehr schlafen.", ChatParser.parse("Mehr schlafen.").reply)
    }

    @Test
    fun longRepliesAreCut() {
        assertEquals(ChatParser.MAX_REPLY_CHARS, ChatParser.parse("""{"reply":"${"a".repeat(5_000)}"}""").reply.length)
    }

    @Test
    fun brokenAnswersAreInvalid() {
        invalid("")
        invalid("""{"reply":""}""")
        invalid("""{"summary":"wrong schema"}""")
        invalid("""{"reply":42}""")
        invalid("""{"reply": "cut off""")
    }
}
