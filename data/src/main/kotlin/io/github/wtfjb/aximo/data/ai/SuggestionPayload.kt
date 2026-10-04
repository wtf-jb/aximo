package io.github.wtfjb.aximo.data.ai

import io.github.wtfjb.aximo.domain.review.SuggestionChange
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Values of a [SuggestionChange] as stored in `ai_suggestions.payloadJson`.
 * One flat shape for all types; [type] in the row says which fields are set.
 */
@Serializable
internal data class SuggestionPayload(
    val routineId: Long,
    val exerciseId: Long,
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
)

internal object SuggestionCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun type(change: SuggestionChange): String = when (change) {
        is SuggestionChange.SetCount -> "SET_COUNT"
        is SuggestionChange.RepRange -> "REP_RANGE"
        is SuggestionChange.TargetRir -> "TARGET_RIR"
        is SuggestionChange.AddExercise -> "ADD_EXERCISE"
        is SuggestionChange.RemoveExercise -> "REMOVE_EXERCISE"
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
        }
        return json.encodeToString(SuggestionPayload.serializer(), payload)
    }

    /** Null if the stored row cannot be read (unknown type or missing field). */
    fun decode(type: String, payloadJson: String): SuggestionChange? {
        val p = try {
            json.decodeFromString(SuggestionPayload.serializer(), payloadJson)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return when (type) {
            "SET_COUNT" -> SuggestionChange.SetCount(p.routineId, p.exerciseId, p.from ?: return null, p.to ?: return null)
            "REP_RANGE" -> SuggestionChange.RepRange(
                p.routineId, p.exerciseId,
                p.fromMin ?: return null, p.fromMax ?: return null, p.toMin ?: return null, p.toMax ?: return null,
            )
            "TARGET_RIR" -> SuggestionChange.TargetRir(p.routineId, p.exerciseId, p.from, p.to)
            "ADD_EXERCISE" -> SuggestionChange.AddExercise(
                p.routineId, p.exerciseId,
                p.sets ?: return null, p.repMin ?: return null, p.repMax ?: return null, p.targetRir,
            )
            "REMOVE_EXERCISE" -> SuggestionChange.RemoveExercise(p.routineId, p.exerciseId)
            else -> null
        }
    }
}
