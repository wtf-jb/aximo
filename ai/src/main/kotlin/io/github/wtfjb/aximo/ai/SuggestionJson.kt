package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.ai.JsonRead.int
import io.github.wtfjb.aximo.ai.JsonRead.long
import io.github.wtfjb.aximo.ai.JsonRead.nullableInt
import io.github.wtfjb.aximo.ai.JsonRead.string
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A plan change in the wire format the review (B-02) and the chat (B-05) share:
 * `{"type": "set_count", "routine_id": …, "rationale": …}`.
 */
internal object SuggestionJson {

    /** One suggestion in the schema of [ReviewPrompt]; null if it does not match. */
    fun parse(o: JsonObject): GeneratedSuggestion? {
        val rationale = o.string("rationale")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val routineId = o.long("routine_id") ?: return null
        val exerciseId = o.long("exercise_id") ?: return null
        val change = when (o.string("type")) {
            "set_count" -> SuggestionChange.SetCount(routineId, exerciseId, o.int("from") ?: return null, o.int("to") ?: return null)
            "rep_range" -> SuggestionChange.RepRange(
                routineId,
                exerciseId,
                fromMin = o.int("from_min") ?: return null,
                fromMax = o.int("from_max") ?: return null,
                toMin = o.int("to_min") ?: return null,
                toMax = o.int("to_max") ?: return null,
            )
            "target_rir" -> {
                val from = o.nullableInt("from") ?: return null
                val to = o.nullableInt("to") ?: return null
                SuggestionChange.TargetRir(routineId, exerciseId, from.value, to.value)
            }
            "add_exercise" -> SuggestionChange.AddExercise(
                routineId,
                exerciseId,
                sets = o.int("sets") ?: return null,
                repMin = o.int("rep_min") ?: return null,
                repMax = o.int("rep_max") ?: return null,
                // Optional: missing means "no target", like null.
                targetRir = if (o.containsKey("target_rir")) (o.nullableInt("target_rir") ?: return null).value else null,
            )
            "remove_exercise" -> SuggestionChange.RemoveExercise(routineId, exerciseId)
            else -> return null
        }
        return GeneratedSuggestion(change, rationale)
    }

    /** The reverse of [parse]: how an earlier chat answer is sent back as history. */
    fun encode(change: SuggestionChange, rationale: String): JsonObject = buildJsonObject {
        put("type", type(change))
        put("routine_id", change.routineId)
        put("exercise_id", change.exerciseId)
        when (change) {
            is SuggestionChange.SetCount -> {
                put("from", change.from)
                put("to", change.to)
            }
            is SuggestionChange.RepRange -> {
                put("from_min", change.fromMin)
                put("from_max", change.fromMax)
                put("to_min", change.toMin)
                put("to_max", change.toMax)
            }
            is SuggestionChange.TargetRir -> {
                put("from", change.from)
                put("to", change.to)
            }
            is SuggestionChange.AddExercise -> {
                put("sets", change.sets)
                put("rep_min", change.repMin)
                put("rep_max", change.repMax)
                put("target_rir", change.targetRir)
            }
            is SuggestionChange.RemoveExercise -> Unit
        }
        put("rationale", rationale)
    }

    private fun type(change: SuggestionChange): String = when (change) {
        is SuggestionChange.SetCount -> "set_count"
        is SuggestionChange.RepRange -> "rep_range"
        is SuggestionChange.TargetRir -> "target_rir"
        is SuggestionChange.AddExercise -> "add_exercise"
        is SuggestionChange.RemoveExercise -> "remove_exercise"
    }
}
