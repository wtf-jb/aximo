package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.ai.JsonRead.int
import io.github.wtfjb.aximo.ai.JsonRead.long
import io.github.wtfjb.aximo.ai.JsonRead.nullableInt
import io.github.wtfjb.aximo.ai.JsonRead.string
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.PlanEntry
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.PlanRoutine
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * A plan change in the wire format the review (B-02) and the chat (B-05) share:
 * `{"type": "set_count", "routine_id": …, "rationale": …}`. Positions in
 * `move_exercise` are 1-based on the wire, 0-based in the app.
 */
internal object SuggestionJson {

    /** Schema lines for the system prompt of the chat; the review prompt keeps its own text. */
    val CHAT_SCHEMA = """
        {"type": "set_count", "routine_id": int, "exercise_id": int, "from": int, "to": int, "rationale": string},
        {"type": "rep_range", "routine_id": int, "exercise_id": int, "from_min": int, "from_max": int, "to_min": int, "to_max": int, "rationale": string},
        {"type": "target_rir", "routine_id": int, "exercise_id": int, "from": int|null, "to": int|null, "rationale": string},
        {"type": "add_exercise", "routine_id": int, "exercise_id": int, "sets": int, "rep_min": int, "rep_max": int, "target_rir": int|null, "rationale": string},
        {"type": "remove_exercise", "routine_id": int, "exercise_id": int, "rationale": string},
        {"type": "replace_exercise", "routine_id": int, "exercise_id": int, "new_exercise_id": int, "rationale": string},
        {"type": "move_exercise", "routine_id": int, "exercise_id": int, "from": int, "to": int, "rationale": string},
        {"type": "rename_routine", "routine_id": int, "from": string, "to": string, "rationale": string},
        {"type": "delete_routine", "routine_id": int, "name": string, "rationale": string},
        {"type": "create_plan", "rationale": string, "routines": [
          {"name": string, "exercises": [
            {"exercise_id": int, "sets": int, "rep_min": int, "rep_max": int, "target_rir": int|null},
            {"new_exercise": {"name": string, "catalog_name": string, "type": "strength"|"bodyweight",
              "equipment": "barbell"|"dumbbell"|"machine"|"cable"|"kettlebell"|"band"|"bodyweight"|"other"},
             "sets": int, "rep_min": int, "rep_max": int, "target_rir": int|null}
          ]}
        ]}
    """.trimIndent()

    /** One suggestion; null if it does not match the schema. */
    fun parse(o: JsonObject): GeneratedSuggestion? {
        val rationale = o.string("rationale")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val type = o.string("type") ?: return null
        if (type == "create_plan") return plan(o)?.let { GeneratedSuggestion(it, rationale) }
        val routineId = o.long("routine_id") ?: return null
        val change = when (type) {
            "rename_routine" -> SuggestionChange.RenameRoutine(routineId, o.string("from") ?: return null, o.string("to") ?: return null)
            "delete_routine" -> SuggestionChange.DeleteRoutine(routineId, o.string("name") ?: return null)
            else -> exerciseChange(type, routineId, o.long("exercise_id") ?: return null, o) ?: return null
        }
        return GeneratedSuggestion(change, rationale)
    }

    private fun exerciseChange(type: String, routineId: Long, exerciseId: Long, o: JsonObject): SuggestionChange.ExerciseChange? = when (type) {
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
            targetRir = (optionalRir(o) ?: return null).value,
        )
        "remove_exercise" -> SuggestionChange.RemoveExercise(routineId, exerciseId)
        "replace_exercise" -> SuggestionChange.ReplaceExercise(routineId, exerciseId, o.long("new_exercise_id") ?: return null)
        "move_exercise" -> SuggestionChange.MoveExercise(
            routineId,
            exerciseId,
            from = (o.int("from") ?: return null) - 1,
            to = (o.int("to") ?: return null) - 1,
        )
        else -> null
    }

    /** `target_rir` is optional: missing means "no target", like null. Wrong type → null (invalid). */
    private fun optionalRir(o: JsonObject): JsonRead.NullableInt? =
        if (o.containsKey("target_rir")) o.nullableInt("target_rir") else JsonRead.NullableInt(null)

    private fun plan(o: JsonObject): SuggestionChange.CreatePlan? {
        val routines = (o["routines"] as? JsonArray ?: return null).mapNotNull { r ->
            val routine = r as? JsonObject ?: return@mapNotNull null
            val name = routine.string("name") ?: return@mapNotNull null
            // Broken entries are left out; a routine without entries is dropped (and counted) in the domain.
            val entries = (routine["exercises"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::planEntry) }
            PlanRoutine(name, entries)
        }
        return routines.takeIf { it.isNotEmpty() }?.let { SuggestionChange.CreatePlan(it) }
    }

    private fun planEntry(o: JsonObject): PlanEntry? {
        val ref = o.long("exercise_id")?.let { PlanExerciseRef.Existing(it) } ?: newExercise(o["new_exercise"] as? JsonObject ?: return null) ?: return null
        return PlanEntry(
            exercise = ref,
            sets = o.int("sets") ?: return null,
            repMin = o.int("rep_min") ?: return null,
            repMax = o.int("rep_max") ?: return null,
            targetRir = (optionalRir(o) ?: return null).value,
        )
    }

    private fun newExercise(o: JsonObject): PlanExerciseRef.New? {
        val name = o.string("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return PlanExerciseRef.New(
            name = name,
            type = if (o.string("type") == "bodyweight") ExerciseType.BODYWEIGHT else ExerciseType.STRENGTH,
            equipment = o.string("equipment")?.let { v -> Equipment.entries.firstOrNull { it.name.equals(v, ignoreCase = true) } } ?: Equipment.OTHER,
            catalogName = o.string("catalog_name")?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    /** The reverse of [parse]: how an earlier chat answer is sent back as history. */
    fun encode(change: SuggestionChange, rationale: String): JsonObject = buildJsonObject {
        put("type", type(change))
        when (change) {
            is SuggestionChange.RoutineChange -> {
                put("routine_id", change.routineId)
                if (change is SuggestionChange.ExerciseChange) put("exercise_id", change.exerciseId)
            }
            is SuggestionChange.CreatePlan -> Unit
        }
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
            is SuggestionChange.ReplaceExercise -> put("new_exercise_id", change.newExerciseId)
            is SuggestionChange.MoveExercise -> {
                put("from", change.from + 1)
                put("to", change.to + 1)
            }
            is SuggestionChange.RenameRoutine -> {
                put("from", change.from)
                put("to", change.to)
            }
            is SuggestionChange.DeleteRoutine -> put("name", change.name)
            is SuggestionChange.CreatePlan -> putJsonArray("routines") {
                change.routines.forEach { routine ->
                    add(
                        buildJsonObject {
                            put("name", routine.name)
                            putJsonArray("exercises") {
                                routine.exercises.forEach { entry -> add(encode(entry)) }
                            }
                        },
                    )
                }
            }
        }
        put("rationale", rationale)
    }

    private fun encode(entry: PlanEntry): JsonObject = buildJsonObject {
        when (val ref = entry.exercise) {
            is PlanExerciseRef.Existing -> put("exercise_id", ref.exerciseId)
            is PlanExerciseRef.New -> putJsonObject("new_exercise") {
                put("name", ref.name)
                ref.catalogName?.let { put("catalog_name", it) }
                put("type", if (ref.type == ExerciseType.BODYWEIGHT) "bodyweight" else "strength")
                put("equipment", ref.equipment.name.lowercase())
            }
        }
        put("sets", entry.sets)
        put("rep_min", entry.repMin)
        put("rep_max", entry.repMax)
        put("target_rir", entry.targetRir)
    }

    private fun type(change: SuggestionChange): String = when (change) {
        is SuggestionChange.SetCount -> "set_count"
        is SuggestionChange.RepRange -> "rep_range"
        is SuggestionChange.TargetRir -> "target_rir"
        is SuggestionChange.AddExercise -> "add_exercise"
        is SuggestionChange.RemoveExercise -> "remove_exercise"
        is SuggestionChange.ReplaceExercise -> "replace_exercise"
        is SuggestionChange.MoveExercise -> "move_exercise"
        is SuggestionChange.RenameRoutine -> "rename_routine"
        is SuggestionChange.DeleteRoutine -> "delete_routine"
        is SuggestionChange.CreatePlan -> "create_plan"
    }
}
