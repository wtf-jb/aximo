package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.ai.JsonRead.int
import io.github.wtfjb.aximo.ai.JsonRead.long
import io.github.wtfjb.aximo.ai.JsonRead.nullableInt
import io.github.wtfjb.aximo.ai.JsonRead.string
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.plan.GeneratedPlan
import io.github.wtfjb.aximo.domain.plan.GeneratedPlanExercise
import io.github.wtfjb.aximo.domain.plan.GeneratedRoutine
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Validates the plan answer against the schema in [PlanPrompt] (B-03). Summary
 * and a routine list are required; a routine or exercise that does not match is
 * dropped and counted. Whether ids and values fit is checked later in the domain
 * ([io.github.wtfjb.aximo.domain.plan.PlanBuilder]).
 */
object PlanParser {

    fun parse(text: String): GeneratedPlan {
        val root = JsonRead.extractObject(text) ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        val summary = root.string("summary")?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        val raw = root["routines"] as? JsonArray ?: throw AiException(AiException.Reason.INVALID_RESPONSE)

        var dropped = 0
        val routines = raw.mapNotNull { element ->
            val parsed = (element as? JsonObject)?.let { routine(it) }
            if (parsed == null) {
                dropped++
                null
            } else {
                dropped += parsed.second
                parsed.first
            }
        }
        return GeneratedPlan(summary, routines, dropped)
    }

    /** The routine and the number of its exercises that were dropped; null if the routine itself is malformed. */
    private fun routine(o: JsonObject): Pair<GeneratedRoutine, Int>? {
        val name = o.string("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val raw = o["exercises"] as? JsonArray ?: return null
        val parsed = raw.map { (it as? JsonObject)?.let(::exercise) }
        return GeneratedRoutine(name, parsed.filterNotNull()) to parsed.count { it == null }
    }

    private fun exercise(o: JsonObject): GeneratedPlanExercise? = GeneratedPlanExercise(
        exerciseId = o.long("exercise_id") ?: return null,
        sets = o.int("sets") ?: return null,
        repMin = o.int("rep_min") ?: return null,
        repMax = o.int("rep_max") ?: return null,
        // Optional: missing means "no target", like null.
        targetRir = if (o.containsKey("target_rir")) (o.nullableInt("target_rir") ?: return null).value else null,
    )
}
