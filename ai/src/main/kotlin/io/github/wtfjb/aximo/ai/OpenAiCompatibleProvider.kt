package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.ktor.client.HttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * `POST {baseUrl}/chat/completions` in OpenAI's format. The base URL includes
 * the version path, e.g. `https://api.openai.com/v1`, `http://192.168.1.10:11434/v1`
 * (Ollama), `https://api.mistral.ai/v1`, `https://openrouter.ai/api/v1`.
 * Without a key no Authorization header is sent (local Ollama).
 */
class OpenAiCompatibleProvider(
    private val client: HttpClient,
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String?,
) : AiProvider {

    override suspend fun complete(request: AiRequest): String {
        val messages = buildList {
            request.system?.let { add(ChatMessage(role = "system", content = it)) }
            request.messages.forEach { add(ChatMessage(role = it.role.wireName(), content = it.text)) }
        }
        val body = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = request.maxTokens,
            responseFormat = if (request.jsonOutput) ResponseFormat(JSON_OBJECT) else null,
        )
        val headers = if (apiKey.isNullOrBlank()) emptyMap() else mapOf("Authorization" to "Bearer $apiKey")
        val text = try {
            post(body, headers)
        } catch (e: AiException) {
            // Not every compatible server knows response_format; it then rejects the request. Try once without.
            if (body.responseFormat == null || e.reason != AiException.Reason.REJECTED) throw e
            post(body.copy(responseFormat = null), headers)
        }

        val response = decodeResponse<ChatResponse>(text)
        // Some gateways (OpenRouter) report errors with status 200.
        response.error?.let { throw AiException(AiException.Reason.REJECTED, detail = it.message?.let { m -> redact(m, apiKey) }) }
        val choice = response.choices?.firstOrNull() ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        if (choice.finishReason == "content_filter") throw AiException(AiException.Reason.REFUSED)
        // Reasoning models may answer with an empty content; that is still a valid answer.
        return choice.message?.text().orEmpty()
    }

    private suspend fun post(body: ChatRequest, headers: Map<String, String>): String =
        client.postJson("$baseUrl/chat/completions", AiHttp.json.encodeToString(ChatRequest.serializer(), body), headers, apiKey)

    private fun AiMessage.Role.wireName() = when (this) {
        AiMessage.Role.USER -> "user"
        AiMessage.Role.ASSISTANT -> "assistant"
    }

    @Serializable
    internal data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        @SerialName("max_tokens") val maxTokens: Int? = null,
        val stream: Boolean = false,
        @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    )

    @Serializable
    internal data class ResponseFormat(val type: String)

    @Serializable
    internal data class ChatMessage(val role: String, val content: String? = null)

    @Serializable
    internal data class ChatResponse(val choices: List<Choice>? = null, val error: ErrorBody? = null)

    @Serializable
    internal data class Choice(val message: AnswerMessage? = null, @SerialName("finish_reason") val finishReason: String? = null)

    /**
     * The answer's content is usually a string. Some models (Mistral's reasoning
     * models) send a list of chunks instead; then the "text" chunks are joined
     * and "thinking" chunks are left out.
     */
    @Serializable
    internal data class AnswerMessage(val content: JsonElement? = null) {
        fun text(): String = when (val c = content) {
            is JsonPrimitive -> if (c.isString) c.content else ""
            is JsonArray -> c.mapNotNull { chunk ->
                (chunk as? JsonObject)?.takeIf { (it["type"] as? JsonPrimitive)?.content == "text" }
                    ?.let { (it["text"] as? JsonPrimitive)?.content }
            }.joinToString("")
            else -> ""
        }
    }

    @Serializable
    internal data class ErrorBody(val message: String? = null)

    private companion object {
        /** `response_format` for "any JSON object"; json_schema is not supported widely enough. */
        const val JSON_OBJECT = "json_object"
    }
}
