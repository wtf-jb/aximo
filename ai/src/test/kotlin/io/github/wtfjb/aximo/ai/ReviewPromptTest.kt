package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.review.AvailableExercise
import io.github.wtfjb.aximo.domain.review.EffortTrend
import io.github.wtfjb.aximo.domain.review.ExerciseTrend
import io.github.wtfjb.aximo.domain.review.RegionVolume
import io.github.wtfjb.aximo.domain.review.ReviewContext
import io.github.wtfjb.aximo.domain.review.RoutineExerciseInfo
import io.github.wtfjb.aximo.domain.review.RoutineInfo
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptTest {

    private val context = ReviewContext(
        today = LocalDate(2026, 10, 4),
        weeks = 6,
        sessions = 4,
        sessionsPerWeek = 0.7,
        weeklyGoal = null,
        routines = listOf(RoutineInfo(7, "Push A", 4, listOf(RoutineExerciseInfo(1, "Bankdrücken", 3, 6, 8, 2)))),
        exercises = listOf(ExerciseTrend(1, "Bankdrücken", 4, ProgressMetric.E1RM, 101.3, 104.5, 104.5, 0, 82.5, 8, 2.0)),
        volume = listOf(RegionVolume(BodyRegion.CHEST, 1.3, 10.0, 20.0)),
        effort = EffortTrend(3.0, null),
        available = listOf(AvailableExercise(1, "Bankdrücken", setOf(BodyRegion.CHEST))),
    )

    @Test
    fun contextJsonHasIdsAndNumbers() {
        val json = Json.parseToJsonElement(ReviewPrompt.contextJson(context).toString()).jsonObject

        assertEquals(ReviewPrompt.VERSION, json["prompt_version"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, json["weekly_goal"])
        val routine = json["routines"]!!.jsonArray[0].jsonObject
        assertEquals(7, routine["routine_id"]!!.jsonPrimitive.content.toInt())
        assertEquals("Bankdrücken", routine["exercises"]!!.jsonArray[0].jsonObject["name"]!!.jsonPrimitive.content)
        val trend = json["exercise_trends"]!!.jsonArray[0].jsonObject
        assertEquals("e1rm_kg", trend["metric"]!!.jsonPrimitive.content)
        assertEquals("82.5 kg x 8", trend["last_top_set"]!!.jsonPrimitive.content)
        assertEquals(1.3, json["volume"]!!.jsonObject["sets_per_week"]!!.jsonObject["chest"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals(JsonNull, json["effort"]!!.jsonObject["avg_rir_second_half"])
        assertEquals("chest", json["available_exercises"]!!.jsonArray[0].jsonObject["regions"]!!.jsonArray[0].jsonPrimitive.content)
    }

    @Test
    fun systemPromptNamesLanguage() {
        assertTrue(ReviewPrompt.system("de").contains("in German"))
        assertTrue(ReviewPrompt.system("en-US").contains("in English"))
        assertTrue(ReviewPrompt.system("fr").contains("in English"))
    }

    @Test
    fun generatorSendsPromptAndParsesAnswer() = runBlocking {
        var sent: AiRequest? = null
        val provider = object : AiProvider {
            override suspend fun complete(request: AiRequest): String {
                sent = request
                return """{"summary":"ok","suggestions":[{"type":"set_count","routine_id":7,"exercise_id":1,"from":3,"to":4,"rationale":"r"}]}"""
            }
        }

        val review = KtorReviewGenerator().generate(provider, context, "de")

        assertEquals(SuggestionChange.SetCount(7, 1, 3, 4), review.suggestions.single().change)
        assertTrue(sent!!.system!!.contains("German"))
        assertEquals(AiMessage.Role.USER, sent!!.messages.single().role)
        assertTrue(sent!!.messages.single().text.contains("\"routine_id\":7"))
    }
}
