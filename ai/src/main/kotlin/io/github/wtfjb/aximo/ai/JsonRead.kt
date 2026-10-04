package io.github.wtfjb.aximo.ai

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull

    fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull

    /** Wraps the value so "null in JSON" (a valid value) differs from "missing or wrong type". */
    fun JsonObject.nullableInt(key: String): NullableInt? = when (val value: JsonElement? = this[key]) {
        JsonNull -> NullableInt(null)
        is JsonPrimitive -> value.takeIf { !it.isString }?.intOrNull?.let { NullableInt(it) }
        else -> null
    }

    data class NullableInt(val value: Int?)
}
