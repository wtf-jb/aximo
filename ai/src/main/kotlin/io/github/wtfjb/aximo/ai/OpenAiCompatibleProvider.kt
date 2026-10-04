package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.ktor.client.HttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        val body = ChatRequest(model = model, messages = messages, maxTokens = request.maxTokens)
        val headers = if (apiKey.isNullOrBlank()) emptyMap() else mapOf("Authorization" to "Bearer $apiKey")
        val text = client.postJson("$baseUrl/chat/completions", AiHttp.json.encodeToString(ChatRequest.serializer(), body), headers, apiKey)

        val response = decodeResponse<ChatResponse>(text)
        // Some gateways (OpenRouter) report errors with status 200.
        response.error?.let { throw AiException(AiException.Reason.REJECTED, detail = it.message?.let { m -> redact(m, apiKey) }) }
        val choice = response.choices?.firstOrNull() ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        if (choice.finishReason == "content_filter") throw AiException(AiException.Reason.REFUSED)
        // Reasoning models may answer with an empty content; that is still a valid answer.
        return choice.message?.content.orEmpty()
    }

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
    )

    @Serializable
    internal data class ChatMessage(val role: String, val content: String? = null)

    @Serializable
    internal data class ChatResponse(val choices: List<Choice>? = null, val error: ErrorBody? = null)

    @Serializable
    internal data class Choice(val message: ChatMessage? = null, @SerialName("finish_reason") val finishReason: String? = null)

    @Serializable
    internal data class ErrorBody(val message: String? = null)
}
