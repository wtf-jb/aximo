package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewParserTest {

    @Test
    fun parsesAllTypes() {
        val review = ReviewParser.parse(
            """
            {"summary": "4 von 4 Einheiten.", "suggestions": [
              {"type":"set_count","routine_id":7,"exercise_id":1,"from":3,"to":4,"rationale":"9 Sätze, Ziel 10–20."},
              {"type":"rep_range","routine_id":7,"exercise_id":2,"from_min":6,"from_max":8,"to_min":8,"to_max":10,"rationale":"Stagniert."},
              {"type":"target_rir","routine_id":7,"exercise_id":1,"from":2,"to":null,"rationale":"r"},
              {"type":"add_exercise","routine_id":8,"exercise_id":5,"sets":3,"rep_min":10,"rep_max":12,"rationale":"r"},
              {"type":"remove_exercise","routine_id":8,"exercise_id":6,"rationale":"r"}
            ]}
            """.trimIndent(),
        )

        assertEquals("4 von 4 Einheiten.", review.summary)
        assertEquals(0, review.dropped)
        assertEquals(
            listOf(
                SuggestionChange.SetCount(7, 1, 3, 4),
                SuggestionChange.RepRange(7, 2, 6, 8, 8, 10),
                SuggestionChange.TargetRir(7, 1, 2, null),
                SuggestionChange.AddExercise(8, 5, 3, 10, 12, null),
                SuggestionChange.RemoveExercise(8, 6),
            ),
            review.suggestions.map { it.change },
        )
        assertEquals("9 Sätze, Ziel 10–20.", review.suggestions[0].rationale)
    }

    @Test
    fun readsTheReasonAndIgnoresUnknownOnes() {
        val review = ReviewParser.parse(
            """
            {"summary": "s", "suggestions": [
              {"type":"set_count","routine_id":7,"exercise_id":1,"from":3,"to":4,"rationale":"r","reason":"volume_low"},
              {"type":"remove_exercise","routine_id":8,"exercise_id":6,"rationale":"r","reason":"boredom"},
              {"type":"remove_exercise","routine_id":8,"exercise_id":7,"rationale":"r"}
            ]}
            """.trimIndent(),
        )

        assertEquals(listOf(SuggestionReason.VOLUME_LOW, null, null), review.suggestions.map { it.reason })
        assertEquals(0, review.dropped)
    }

    @Test
    fun acceptsFencesAndTextAround() {
        val review = ReviewParser.parse("Here you go:\n```json\n{\"summary\":\"ok\",\"suggestions\":[]}\n```")

        assertEquals("ok", review.summary)
        assertTrue(review.suggestions.isEmpty())
    }

    @Test
    fun dropsInvalidSuggestionsKeepsRest() {
        val review = ReviewParser.parse(
            """
            {"summary":"s","suggestions":[
              {"type":"set_count","routine_id":7,"exercise_id":1,"from":3,"to":4,"rationale":"ok"},
              {"type":"set_count","routine_id":7,"exercise_id":1,"from":"3","to":4,"rationale":"string number"},
              {"type":"set_count","routine_id":7,"exercise_id":1,"from":3,"to":4},
              {"type":"deload","routine_id":7,"exercise_id":1,"rationale":"unknown type"},
              {"type":"target_rir","routine_id":7,"exercise_id":1,"to":1,"rationale":"from missing"},
              "not an object"
            ]}
            """.trimIndent(),
        )

        assertEquals(1, review.suggestions.size)
        assertEquals(5, review.dropped)
    }

    @Test
    fun capsSuggestions() {
        val one = """{"type":"remove_exercise","routine_id":1,"exercise_id":1,"rationale":"r"}"""
        val review = ReviewParser.parse("""{"summary":"s","suggestions":[${List(7) { one }.joinToString(",")}]}""")

        assertEquals(ReviewPrompt.MAX_SUGGESTIONS, review.suggestions.size)
        assertEquals(2, review.dropped)
    }

    @Test
    fun missingSummaryOrNoJsonIsInvalid() {
        listOf("""{"suggestions":[]}""", """{"summary":"  "}""", "Sorry, I can't.", "[1,2]", "{broken").forEach { text ->
            val error = try {
                ReviewParser.parse(text)
                null
            } catch (e: AiException) {
                e
            }
            assertEquals(text, AiException.Reason.INVALID_RESPONSE, error?.reason)
        }
    }

    @Test
    fun missingSuggestionsIsEmpty() {
        assertTrue(ReviewParser.parse("""{"summary":"Alles im Plan."}""").suggestions.isEmpty())
    }
}
