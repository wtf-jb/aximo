package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** Small helpers the answer parsers share: strict field access on a [JsonObject]. */
internal object JsonRead {

    /**
     * The JSON object in the answer. Models sometimes wrap it in ``` fences or
     * a sentence, so everything from the first `{` to the last `}` is tried.
     */
    fun extractObject(text: String): JsonObject? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            AiHttp.json.parseToJsonElement(text.substring(start, end + 1)) as? JsonObject
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * INVALID_RESPONSE with the start of the answer as detail, so the user can
     * see what the model sent instead (shown under the error message).
     */
    fun invalid(text: String): AiException {
        val start = text.trim().replace(Regex("\\s+"), " ")
        return AiException(
            AiException.Reason.INVALID_RESPONSE,
            detail = start.takeIf { it.isNotEmpty() }?.let { if (it.length > MAX_SNIPPET) it.take(MAX_SNIPPET) + " …" else it },
        )
    }

    private const val MAX_SNIPPET = 200

    fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull

    fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull

    fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull

    /** Wraps the value so "null in JSON" (a valid value) differs from "missing or wrong type". */
    fun JsonObject.nullableInt(key: String): NullableInt? = when (val value: JsonElement? = this[key]) {
        JsonNull -> NullableInt(null)
        is JsonPrimitive -> value.takeIf { !it.isString }?.intOrNull?.let { NullableInt(it) }
        else -> null
    }

    data class NullableInt(val value: Int?)

    /** Like [nullableInt] for decimals. */
    fun JsonObject.nullableDouble(key: String): NullableDouble? = when (val value: JsonElement? = this[key]) {
        JsonNull -> NullableDouble(null)
        is JsonPrimitive -> value.takeIf { !it.isString }?.doubleOrNull?.let { NullableDouble(it) }
        else -> null
    }

    data class NullableDouble(val value: Double?)
}
