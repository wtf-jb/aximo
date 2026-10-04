package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.review.GeneratedReview
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.ai.JsonRead.int
import io.github.wtfjb.aximo.ai.JsonRead.long
import io.github.wtfjb.aximo.ai.JsonRead.nullableInt
import io.github.wtfjb.aximo.ai.JsonRead.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Validates the review answer against the schema in [ReviewPrompt] (B-02).
 * The summary is required; a suggestion that does not match is dropped and
 * counted, the rest is kept. Whether ids and `from` values fit the routines is
 * checked later in the domain ([io.github.wtfjb.aximo.domain.review.SuggestionApplier]).
 */
object ReviewParser {

    fun parse(text: String): GeneratedReview {
        val root = JsonRead.extractObject(text) ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        val summary = root.string("summary")?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        val raw = (root["suggestions"] as? JsonArray).orEmpty()
        val parsed = raw.take(ReviewPrompt.MAX_SUGGESTIONS).map { (it as? JsonObject)?.let(::suggestion) }
        return GeneratedReview(
            summary = summary,
            suggestions = parsed.filterNotNull(),
            dropped = parsed.count { it == null } + (raw.size - parsed.size).coerceAtLeast(0),
        )
    }

    private fun suggestion(o: JsonObject): GeneratedSuggestion? {
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
}
