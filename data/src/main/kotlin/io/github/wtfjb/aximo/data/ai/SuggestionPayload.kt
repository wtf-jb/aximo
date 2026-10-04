package io.github.wtfjb.aximo.data.ai

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.review.PlanEntry
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.PlanRoutine
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Values of a [SuggestionChange] as stored in `ai_suggestions.payloadJson`.
 * One flat shape for all types; [type] in the row says which fields are set.
 * New fields are optional, so rows written by older versions still read.
 */
@Serializable
internal data class SuggestionPayload(
    val routineId: Long? = null,
    val exerciseId: Long? = null,
    val from: Int? = null,
    val to: Int? = null,
    val fromMin: Int? = null,
    val fromMax: Int? = null,
    val toMin: Int? = null,
    val toMax: Int? = null,
    val sets: Int? = null,
    val repMin: Int? = null,
    val repMax: Int? = null,
    val targetRir: Int? = null,
    val newExerciseId: Long? = null,
    val fromName: String? = null,
    val toName: String? = null,
    val routines: List<PlanRoutinePayload>? = null,
)

@Serializable
internal data class PlanRoutinePayload(val name: String, val exercises: List<PlanEntryPayload>)

/** [exerciseId] for an existing exercise, otherwise the fields of a new one. */
@Serializable
internal data class PlanEntryPayload(
    val exerciseId: Long? = null,
    val newName: String? = null,
    val newType: String? = null,
    val newEquipment: String? = null,
    val catalogId: String? = null,
    val catalogName: String? = null,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int? = null,
)

internal object SuggestionCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun type(change: SuggestionChange): String = when (change) {
        is SuggestionChange.SetCount -> "SET_COUNT"
        is SuggestionChange.RepRange -> "REP_RANGE"
        is SuggestionChange.TargetRir -> "TARGET_RIR"
        is SuggestionChange.AddExercise -> "ADD_EXERCISE"
        is SuggestionChange.RemoveExercise -> "REMOVE_EXERCISE"
        is SuggestionChange.ReplaceExercise -> "REPLACE_EXERCISE"
        is SuggestionChange.MoveExercise -> "MOVE_EXERCISE"
        is SuggestionChange.RenameRoutine -> "RENAME_ROUTINE"
        is SuggestionChange.DeleteRoutine -> "DELETE_ROUTINE"
        is SuggestionChange.CreatePlan -> "CREATE_PLAN"
    }

    fun encode(change: SuggestionChange): String {
        val payload = when (change) {
            is SuggestionChange.SetCount -> SuggestionPayload(change.routineId, change.exerciseId, from = change.from, to = change.to)
            is SuggestionChange.RepRange -> SuggestionPayload(
                change.routineId, change.exerciseId,
                fromMin = change.fromMin, fromMax = change.fromMax, toMin = change.toMin, toMax = change.toMax,
            )
            is SuggestionChange.TargetRir -> SuggestionPayload(change.routineId, change.exerciseId, from = change.from, to = change.to)
            is SuggestionChange.AddExercise -> SuggestionPayload(
                change.routineId, change.exerciseId,
                sets = change.sets, repMin = change.repMin, repMax = change.repMax, targetRir = change.targetRir,
            )
            is SuggestionChange.RemoveExercise -> SuggestionPayload(change.routineId, change.exerciseId)
            is SuggestionChange.ReplaceExercise -> SuggestionPayload(change.routineId, change.exerciseId, newExerciseId = change.newExerciseId)
            is SuggestionChange.MoveExercise -> SuggestionPayload(change.routineId, change.exerciseId, from = change.from, to = change.to)
            is SuggestionChange.RenameRoutine -> SuggestionPayload(change.routineId, fromName = change.from, toName = change.to)
            is SuggestionChange.DeleteRoutine -> SuggestionPayload(change.routineId, fromName = change.name)
            is SuggestionChange.CreatePlan -> SuggestionPayload(routines = change.routines.map { r -> PlanRoutinePayload(r.name, r.exercises.map(::entry)) })
        }
        return json.encodeToString(SuggestionPayload.serializer(), payload)
    }

    private fun entry(e: PlanEntry): PlanEntryPayload = when (val ref = e.exercise) {
        is PlanExerciseRef.Existing -> PlanEntryPayload(exerciseId = ref.exerciseId, sets = e.sets, repMin = e.repMin, repMax = e.repMax, targetRir = e.targetRir)
        is PlanExerciseRef.New -> PlanEntryPayload(
            newName = ref.name, newType = ref.type.name, newEquipment = ref.equipment.name,
            catalogId = ref.catalogId, catalogName = ref.catalogName,
            sets = e.sets, repMin = e.repMin, repMax = e.repMax, targetRir = e.targetRir,
        )
    }

    /** Null if the stored row cannot be read (unknown type or missing field). */
    fun decode(type: String, payloadJson: String): SuggestionChange? {
        val p = try {
            json.decodeFromString(SuggestionPayload.serializer(), payloadJson)
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (type == "CREATE_PLAN") {
            val routines = p.routines ?: return null
            return SuggestionChange.CreatePlan(routines.map { r -> PlanRoutine(r.name, r.exercises.map { decodeEntry(it) ?: return null }) })
        }
        val routineId = p.routineId ?: return null
        return when (type) {
            "RENAME_ROUTINE" -> SuggestionChange.RenameRoutine(routineId, p.fromName ?: return null, p.toName ?: return null)
            "DELETE_ROUTINE" -> SuggestionChange.DeleteRoutine(routineId, p.fromName ?: return null)
            else -> decodeExerciseChange(type, routineId, p.exerciseId ?: return null, p)
        }
    }

    private fun decodeExerciseChange(type: String, routineId: Long, exerciseId: Long, p: SuggestionPayload): SuggestionChange? = when (type) {
        "SET_COUNT" -> SuggestionChange.SetCount(routineId, exerciseId, p.from ?: return null, p.to ?: return null)
        "REP_RANGE" -> SuggestionChange.RepRange(
            routineId, exerciseId,
            p.fromMin ?: return null, p.fromMax ?: return null, p.toMin ?: return null, p.toMax ?: return null,
        )
        "TARGET_RIR" -> SuggestionChange.TargetRir(routineId, exerciseId, p.from, p.to)
        "ADD_EXERCISE" -> SuggestionChange.AddExercise(
            routineId, exerciseId,
            p.sets ?: return null, p.repMin ?: return null, p.repMax ?: return null, p.targetRir,
        )
        "REMOVE_EXERCISE" -> SuggestionChange.RemoveExercise(routineId, exerciseId)
        "REPLACE_EXERCISE" -> SuggestionChange.ReplaceExercise(routineId, exerciseId, p.newExerciseId ?: return null)
        "MOVE_EXERCISE" -> SuggestionChange.MoveExercise(routineId, exerciseId, p.from ?: return null, p.to ?: return null)
        else -> null
    }

    private fun decodeEntry(e: PlanEntryPayload): PlanEntry? {
        val ref = e.exerciseId?.let { PlanExerciseRef.Existing(it) } ?: PlanExerciseRef.New(
            name = e.newName ?: return null,
            type = ExerciseType.entries.firstOrNull { it.name == e.newType } ?: return null,
            equipment = Equipment.entries.firstOrNull { it.name == e.newEquipment } ?: return null,
            catalogId = e.catalogId,
            catalogName = e.catalogName,
        )
        return PlanEntry(ref, e.sets, e.repMin, e.repMax, e.targetRir)
    }
}
