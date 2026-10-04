package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.plan.PlanExercise
import io.github.wtfjb.aximo.domain.plan.PlanGoal
import io.github.wtfjb.aximo.domain.plan.PlanInput
import io.github.wtfjb.aximo.domain.plan.PlanRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanPromptTest {

    private val input = PlanInput(
        PlanRequest(goal = PlanGoal.STRENGTH, daysPerWeek = 4, minutes = 45, restrictions = " Schulter links "),
        listOf(PlanExercise(1, "Bankdrücken", Equipment.BARBELL, setOf(BodyRegion.CHEST, BodyRegion.ARMS), 6, 10)),
    )

    @Test
    fun inputJsonHasWishesAndExercises() {
        val json = Json.parseToJsonElement(PlanPrompt.inputJson(input).toString()).jsonObject

        assertEquals(PlanPrompt.VERSION, json["prompt_version"]!!.jsonPrimitive.content)
        assertEquals("strength", json["goal"]!!.jsonPrimitive.content)
        assertEquals(4, json["days_per_week"]!!.jsonPrimitive.content.toInt())
        assertEquals(45, json["minutes_per_session"]!!.jsonPrimitive.content.toInt())
        assertEquals(5, json["exercises_per_routine"]!!.jsonPrimitive.content.toInt())
        assertEquals("Schulter links", json["restrictions"]!!.jsonPrimitive.content)
        val exercise = json["available_exercises"]!!.jsonArray[0].jsonObject
        assertEquals(1, exercise["exercise_id"]!!.jsonPrimitive.content.toInt())
        assertEquals("barbell", exercise["equipment"]!!.jsonPrimitive.content)
        assertEquals(listOf("chest", "arms"), exercise["regions"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun noTrainingHistoryIsSent() {
        val text = PlanPrompt.prettyInput(input)

        assertFalse(text.contains("sessions"))
        assertFalse(text.contains("weight"))
    }

    @Test
    fun systemPromptNamesLanguage() {
        assertTrue(PlanPrompt.system("de").contains("in German"))
        assertTrue(PlanPrompt.system("en").contains("in English"))
    }

    @Test
    fun generatorSendsPromptAndParsesAnswer() = runBlocking {
        var sent: AiRequest? = null
        val provider = object : AiProvider {
            override suspend fun complete(request: AiRequest): String {
                sent = request
                return """{"summary":"ok","routines":[{"name":"A","exercises":[{"exercise_id":1,"sets":3,"rep_min":6,"rep_max":8,"target_rir":2}]}]}"""
            }
        }

        val plan = KtorPlanGenerator().generate(provider, input, "de")

        assertEquals(1L, plan.routines.single().exercises.single().exerciseId)
        assertTrue(sent!!.system!!.contains("German"))
        assertEquals(AiMessage.Role.USER, sent!!.messages.single().role)
        assertTrue(sent!!.messages.single().text.contains("\"exercise_id\":1"))
    }
}
