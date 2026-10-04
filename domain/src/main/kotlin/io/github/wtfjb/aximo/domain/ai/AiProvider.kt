package io.github.wtfjb.aximo.domain.ai

/** Which wire format a provider speaks (B-01). */
enum class AiProviderKind {
    /** `/chat/completions` as OpenAI defines it; also Ollama, Mistral, OpenRouter and others. */
    OPENAI_COMPATIBLE,

    /** Anthropic's native Messages API. */
    ANTHROPIC,
}

/** One message of a conversation sent to the AI. */
data class AiMessage(val role: Role, val text: String) {
    enum class Role { USER, ASSISTANT }
}

/**
 * A request to the AI. [maxTokens] null lets the provider decide; the Anthropic
 * API needs a value, so its adapter uses its own default then.
 */
data class AiRequest(
    val messages: List<AiMessage>,
    val system: String? = null,
    val maxTokens: Int? = null,
) {
    init {
        require(messages.isNotEmpty()) { "A request needs at least one message" }
        require(maxTokens == null || maxTokens > 0) { "maxTokens must be positive" }
    }
}

/**
 * A configured AI endpoint. Implementations live in `:ai`; the rest of the app
 * only knows this interface. Throws [AiException] on every failure.
 */
interface AiProvider {
    /** Sends [request] and returns the answer text. */
    suspend fun complete(request: AiRequest): String
}

/** Creates a provider for a profile and its API key (null = no key, e.g. a local Ollama). */
fun interface AiProviderFactory {
    fun create(profile: AiProviderProfile, apiKey: String?): AiProvider
}

/**
 * Why an AI request failed. [detail] is the provider's own error message, if it
 * sent one, with the API key removed; it is never logged.
 */
class AiException(
    val reason: Reason,
    val statusCode: Int? = null,
    val detail: String? = null,
    cause: Throwable? = null,
) : Exception("AI request failed: $reason${statusCode?.let { " (HTTP $it)" } ?: ""}", cause) {

    enum class Reason {
        /** 401/403: key missing, wrong or without permission. */
        UNAUTHORIZED,

        /** 404: wrong base URL or unknown model. */
        NOT_FOUND,

        /** 429: too many requests or quota used up. */
        RATE_LIMITED,

        /** Other 4xx: the provider rejected the request (e.g. model does not exist). */
        REJECTED,

        /** 5xx or overloaded. */
        SERVER,

        /** No answer in time. */
        TIMEOUT,

        /** No connection: offline, unknown host, refused, TLS failed. */
        NETWORK,

        /** Answer arrived but did not have the expected shape. */
        INVALID_RESPONSE,

        /** The model declined to answer. */
        REFUSED,
    }
}
