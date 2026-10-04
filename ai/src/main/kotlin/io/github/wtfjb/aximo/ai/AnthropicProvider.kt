package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiMessage
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.ktor.client.HttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Anthropic's Messages API: `POST {baseUrl}/v1/messages` with `x-api-key` and
 * `anthropic-version`. Base URL is normally `https://api.anthropic.com`; a
 * trailing `/v1` is accepted too.
 */
class AnthropicProvider(
    private val client: HttpClient,
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String?,
) : AiProvider {

    override suspend fun complete(request: AiRequest): String {
        val body = MessagesRequest(
            model = model,
            maxTokens = request.maxTokens ?: DEFAULT_MAX_TOKENS,
            system = request.system,
            messages = request.messages.map { Message(role = it.role.wireName(), content = it.text) },
        )
        val headers = buildMap {
            put("anthropic-version", API_VERSION)
            if (!apiKey.isNullOrBlank()) put("x-api-key", apiKey)
        }
        val text = client.postJson(messagesUrl(), AiHttp.json.encodeToString(MessagesRequest.serializer(), body), headers, apiKey)

        val response = decodeResponse<MessagesResponse>(text)
        if (response.stopReason == "refusal") throw AiException(AiException.Reason.REFUSED)
        val content = response.content ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        // Only text blocks are the answer; thinking blocks and others are skipped.
        return content.filter { it.type == "text" }.joinToString("") { it.text.orEmpty() }
    }

    private fun messagesUrl(): String =
        if (baseUrl.endsWith("/v1")) "$baseUrl/messages" else "$baseUrl/v1/messages"

    private fun AiMessage.Role.wireName() = when (this) {
        AiMessage.Role.USER -> "user"
        AiMessage.Role.ASSISTANT -> "assistant"
    }

    @Serializable
    internal data class MessagesRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String? = null,
        val messages: List<Message>,
    )

    @Serializable
    internal data class Message(val role: String, val content: String)

    @Serializable
    internal data class MessagesResponse(
        val content: List<ContentBlock>? = null,
        @SerialName("stop_reason") val stopReason: String? = null,
    )

    @Serializable
    internal data class ContentBlock(val type: String, val text: String? = null)

    companion object {
        const val API_VERSION = "2023-06-01"

        /** Required by the API. Generous, so a longer answer is not cut off. */
        const val DEFAULT_MAX_TOKENS = 16_000
    }
}
