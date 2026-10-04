package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.ai.JsonRead.int
import io.github.wtfjb.aximo.ai.JsonRead.nullableDouble
import io.github.wtfjb.aximo.ai.JsonRead.nullableInt
import io.github.wtfjb.aximo.ai.JsonRead.string
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.logging.ParsedExercise
import io.github.wtfjb.aximo.domain.logging.ParsedLog
import io.github.wtfjb.aximo.domain.logging.ParsedSetGroup
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.units.WeightUnit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Validates the logging answer against the schema in [LoggingPrompt] (B-04). The
 * exercise list is required (empty is fine: nothing recognised); an exercise or set
 * object that does not match is dropped and counted. Whether the values are in range
 * and which exercise a name means is checked later in the domain
 * ([io.github.wtfjb.aximo.domain.logging.LoggingBuilder]).
 */
object LoggingParser {

    fun parse(text: String): ParsedLog {
        val root = JsonRead.extractObject(text) ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        val raw = root["exercises"] as? JsonArray ?: throw AiException(AiException.Reason.INVALID_RESPONSE)

        var dropped = 0
        val exercises = raw.mapNotNull { element ->
            val parsed = (element as? JsonObject)?.let { exercise(it) }
            if (parsed == null) {
                dropped++
                null
            } else {
                dropped += parsed.second
                parsed.first
            }
        }
        return ParsedLog(exercises, dropped)
    }

    /** The exercise and the number of its set objects that were dropped; null if the exercise itself is malformed. */
    private fun exercise(o: JsonObject): Pair<ParsedExercise, Int>? {
        val name = o.string("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val raw = o["sets"] as? JsonArray ?: return null
        val sets = raw.map { (it as? JsonObject)?.let(::group) }
        // Hints are optional: anything unknown counts as "not said".
        val exercise = ParsedExercise(
            name = name,
            type = when (o.string("type")?.lowercase()) {
                "strength" -> ExerciseType.STRENGTH
                "bodyweight" -> ExerciseType.BODYWEIGHT
                else -> null
            },
            equipment = o.string("equipment")?.let { value -> Equipment.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } },
            sets = sets.filterNotNull(),
        )
        return exercise to sets.count { it == null }
    }

    private fun group(o: JsonObject): ParsedSetGroup? = ParsedSetGroup(
        // Optional: missing or null means one set.
        count = if (o.containsKey("count")) (o.nullableInt("count") ?: return null).value ?: 1 else 1,
        weight = if (o.containsKey("weight")) (o.nullableDouble("weight") ?: return null).value else null,
        weightUnit = if (o.isPresent("weight_unit")) unit(o.string("weight_unit") ?: return null) ?: return null else null,
        reps = o.int("reps") ?: return null,
        rir = if (o.containsKey("rir")) (o.nullableInt("rir") ?: return null).value else null,
        rpe = if (o.containsKey("rpe")) (o.nullableDouble("rpe") ?: return null).value else null,
        setType = if (o.isPresent("set_type")) setType(o.string("set_type") ?: return null) ?: return null else SetType.WORKING,
    )

    /** The key exists and is not JSON null. */
    private fun JsonObject.isPresent(key: String): Boolean = this[key]?.let { it !is JsonNull } ?: false

    private fun unit(value: String): WeightUnit? = when (value.trim().lowercase()) {
        "kg" -> WeightUnit.KG
        "lbs", "lb" -> WeightUnit.LBS
        else -> null
    }

    private fun setType(value: String): SetType? = when (value.trim().lowercase()) {
        "working" -> SetType.WORKING
        "warm_up", "warmup" -> SetType.WARM_UP
        "drop" -> SetType.DROP
        "failure" -> SetType.FAILURE
        else -> null
    }
}
