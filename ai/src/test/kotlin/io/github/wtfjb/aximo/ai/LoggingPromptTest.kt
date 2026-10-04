package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.logging.LoggingExercise
import io.github.wtfjb.aximo.domain.logging.LoggingInput
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggingPromptTest {

    private val input = LoggingInput(
        text = "Bankdrücken 3x8 80 kg RIR 2",
        exercises = listOf(LoggingExercise("Bankdrücken", Equipment.BARBELL), LoggingExercise("Klimmzug", Equipment.BODYWEIGHT)),
        unit = WeightUnit.LBS,
        rating = SetRating.RPE,
    )

    @Test
    fun inputJsonHasTextNamesAndSettings() {
        val json = Json.parseToJsonElement(LoggingPrompt.inputJson(input).toString()).jsonObject

        assertEquals(LoggingPrompt.VERSION, json["prompt_version"]!!.jsonPrimitive.content)
        assertEquals("Bankdrücken 3x8 80 kg RIR 2", json["text"]!!.jsonPrimitive.content)
        assertEquals("lbs", json["default_weight_unit"]!!.jsonPrimitive.content)
        assertEquals("rpe", json["default_effort_scale"]!!.jsonPrimitive.content)
        val exercises = json["known_exercises"]!!.jsonArray
        assertEquals(listOf("Bankdrücken", "Klimmzug"), exercises.map { it.jsonObject["name"]!!.jsonPrimitive.content })
        assertEquals("barbell", exercises[0].jsonObject["equipment"]!!.jsonPrimitive.content)
    }

    @Test
    fun onlyTextNamesAndSettingsAreSent() {
        val json = Json.parseToJsonElement(LoggingPrompt.inputJson(input).toString()).jsonObject

        assertEquals(setOf("prompt_version", "text", "default_weight_unit", "default_effort_scale", "known_exercises"), json.keys)
        assertEquals(setOf("name", "equipment"), json["known_exercises"]!!.jsonArray[0].jsonObject.keys)
    }

    @Test
    fun defaultsAreKgAndRir() {
        val json = Json.parseToJsonElement(LoggingPrompt.inputJson(input.copy(unit = WeightUnit.KG, rating = SetRating.RIR)).toString()).jsonObject

        assertEquals("kg", json["default_weight_unit"]!!.jsonPrimitive.content)
        assertEquals("rir", json["default_effort_scale"]!!.jsonPrimitive.content)
    }

    @Test
    fun prettyInputIsTheSameJsonIndented() {
        val pretty = LoggingPrompt.prettyInput(input)

        assertTrue(pretty.contains("\n"))
        assertEquals(LoggingPrompt.inputJson(input), Json.parseToJsonElement(pretty))
        assertFalse(pretty.contains("exercise_id"))
    }

    @Test
    fun systemPromptNamesLanguageAndSchema() {
        assertTrue(LoggingPrompt.system("de").contains("in German"))
        assertTrue(LoggingPrompt.system("en-US").contains("in English"))
        assertTrue(LoggingPrompt.system("de").contains("\"exercises\""))
    }

    @Test
    fun generatorSendsPromptAndParsesAnswer() = runBlocking {
        var sent: AiRequest? = null
        val provider = object : AiProvider {
            override suspend fun complete(request: AiRequest): String {
                sent = request
                return """{"exercises":[{"name":"Bankdrücken","sets":[{"count":3,"weight":80,"reps":8,"rir":2}]}]}"""
            }
        }

        val log = KtorLoggingGenerator().parse(provider, input, "de")

        assertEquals(3, log.exercises.single().sets.single().count)
        assertTrue(sent!!.system!!.contains("German"))
        assertEquals(AiMessage.Role.USER, sent!!.messages.single().role)
        assertTrue(sent!!.messages.single().text.contains("\"text\":\"Bankdrücken 3x8 80 kg RIR 2\""))
    }
}
