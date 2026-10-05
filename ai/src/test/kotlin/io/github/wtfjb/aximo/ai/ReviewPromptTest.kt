package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.review.AvailableExercise
import io.github.wtfjb.aximo.domain.review.EffortStatus
import io.github.wtfjb.aximo.domain.review.EffortTrend
import io.github.wtfjb.aximo.domain.review.ExerciseTrend
import io.github.wtfjb.aximo.domain.review.RegionVolume
import io.github.wtfjb.aximo.domain.review.ReviewContext
import io.github.wtfjb.aximo.domain.review.RoutineExerciseInfo
import io.github.wtfjb.aximo.domain.review.RoutineInfo
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.TrendStatus
import io.github.wtfjb.aximo.domain.review.VolumeStatus
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
        exercises = listOf(
            ExerciseTrend(
                1, "Bankdrücken", 4, ProgressMetric.E1RM, 101.3, 104.5, 104.5, 0, 82.5, 8, 2.0,
                recent = listOf(101.3, 103.0, 104.5), changePct = 3.2, dropFromBestPct = 0.0, daysSinceLast = 3,
                status = TrendStatus.PROGRESSING, newBest = true, repMax = 8, targetRir = 2, sessionsAtRepCeiling = 2,
                recentAvgRir = 1.7, rirVsTarget = -0.3, effort = EffortStatus.ON_TARGET,
            ),
            ExerciseTrend(2, "Klimmzug", 2, ProgressMetric.REPS, 8.0, 8.0, 8.0, 0, 0.0, 8, null),
        ),
        volume = listOf(RegionVolume(BodyRegion.CHEST, 1.3, 10.0, 20.0, VolumeStatus.BELOW)),
        effort = EffortTrend(3.0, null),
        daysSinceLastSession = 3,
        longestBreakDays = 9,
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
        val chest = json["volume"]!!.jsonObject["sets_per_week"]!!.jsonObject["chest"]!!.jsonObject
        assertEquals(1.3, chest["sets"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals("below", chest["status"]!!.jsonPrimitive.content)
        assertEquals(3, json["days_since_last_session"]!!.jsonPrimitive.content.toInt())
        assertEquals(9, json["longest_break_days"]!!.jsonPrimitive.content.toInt())
        assertEquals(JsonNull, json["effort"]!!.jsonObject["avg_rir_second_half"])
        assertEquals("chest", json["available_exercises"]!!.jsonArray[0].jsonObject["regions"]!!.jsonArray[0].jsonPrimitive.content)
    }

    @Test
    fun contextJsonHasTheClassifiedSignals() {
        val trends = ReviewPrompt.contextJson(context)["exercise_trends"]!!.jsonArray

        val bench = trends[0].jsonObject
        assertEquals("progressing", bench["status"]!!.jsonPrimitive.content)
        assertEquals(listOf(101.3, 103.0, 104.5), bench["recent"]!!.jsonArray.map { it.jsonPrimitive.content.toDouble() })
        assertEquals(3.2, bench["change_pct"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals(0.0, bench["drop_from_best_pct"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals(3, bench["days_since_last"]!!.jsonPrimitive.content.toInt())
        assertEquals("true", bench["new_best"]!!.jsonPrimitive.content)
        assertEquals(8, bench["rep_max"]!!.jsonPrimitive.content.toInt())
        assertEquals(2, bench["target_rir"]!!.jsonPrimitive.content.toInt())
        assertEquals(2, bench["sessions_at_rep_ceiling"]!!.jsonPrimitive.content.toInt())
        assertEquals(1.7, bench["recent_avg_rir"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals(-0.3, bench["rir_vs_target"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals("on_target", bench["effort"]!!.jsonPrimitive.content)

        // No routine target: the target fields are null.
        val pullUp = trends[1].jsonObject
        assertEquals("too_few_data", pullUp["status"]!!.jsonPrimitive.content)
        listOf("rep_max", "target_rir", "rir_vs_target", "effort", "change_pct").forEach { assertEquals(it, JsonNull, pullUp[it]) }
    }

    @Test
    fun versionAndRules() {
        assertEquals("review-v2", ReviewPrompt.VERSION)
        val system = ReviewPrompt.system("en")
        listOf("regressing", "returning", "sessions_at_rep_ceiling", "\"reason\"", "too_hard", "new_best")
            .forEach { assertTrue(it, system.contains(it)) }
        assertTrue(!system.contains("Look for stagnation"))
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
