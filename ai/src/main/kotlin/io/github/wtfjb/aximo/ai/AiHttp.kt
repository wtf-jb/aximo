package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException

/** How long to wait. LLM answers can take a while, so the request timeout is generous. */
data class AiTimeouts(
    val connectMillis: Long = 15_000,
    val socketMillis: Long = 90_000,
    val requestMillis: Long = 180_000,
)

object AiHttp {
    /**
     * The HTTP client for all providers. No logging plugin on purpose: requests
     * carry the API key and training data.
     */
    fun client(engine: HttpClientEngine, timeouts: AiTimeouts = AiTimeouts()): HttpClient = HttpClient(engine) {
        // Status codes are mapped to AiException below instead of Ktor's exceptions.
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = timeouts.connectMillis
            socketTimeoutMillis = timeouts.socketMillis
            requestTimeoutMillis = timeouts.requestMillis
        }
    }

    internal val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false // optional fields like max_tokens are left out instead of sent as null
        encodeDefaults = true
    }
}

/**
 * POSTs [body] as JSON and returns the response text. Every failure becomes an
 * [AiException]; [apiKey] is removed from any error text the server sends back.
 */
internal suspend fun HttpClient.postJson(
    url: String,
    body: String,
    headers: Map<String, String>,
    apiKey: String?,
): String = send(apiKey) {
    post(url) {
        headers.forEach { (name, value) -> header(name, value) }
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}

/** GETs [url] and returns the response text, with the same error handling as [postJson]. */
internal suspend fun HttpClient.getJson(url: String, headers: Map<String, String>, apiKey: String?): String = send(apiKey) {
    get(url) { headers.forEach { (name, value) -> header(name, value) } }
}

private suspend fun send(apiKey: String?, call: suspend () -> HttpResponse): String {
    try {
        val response = call()
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw statusError(response.status.value, text, apiKey)
        return text
    } catch (e: AiException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        throw AiException(AiException.Reason.TIMEOUT, cause = e)
    } catch (e: ConnectTimeoutException) {
        throw AiException(AiException.Reason.TIMEOUT, cause = e)
    } catch (e: SocketTimeoutException) {
        throw AiException(AiException.Reason.TIMEOUT, cause = e)
    } catch (e: CancellationException) {
        throw e // the caller left the screen; not an error
    } catch (e: IOException) {
        throw AiException(AiException.Reason.NETWORK, cause = e)
    } catch (e: IllegalArgumentException) {
        // Ktor rejects URLs it cannot parse; the form checks them, so this is rare.
        throw AiException(AiException.Reason.NETWORK, cause = e)
    }
}

/** Maps an HTTP error status to a reason. */
internal fun statusError(status: Int, body: String, apiKey: String?): AiException {
    val reason = when {
        status == 401 || status == 403 -> AiException.Reason.UNAUTHORIZED
        status == 404 -> AiException.Reason.NOT_FOUND
        status == 408 -> AiException.Reason.TIMEOUT
        status == 429 -> AiException.Reason.RATE_LIMITED
        status >= 500 -> AiException.Reason.SERVER // includes Anthropic's 529 "overloaded"
        else -> AiException.Reason.REJECTED
    }
    return AiException(reason, statusCode = status, detail = errorDetail(body, apiKey))
}

/**
 * The provider's error message, if the body is JSON with one of the usual
 * shapes: `{"error":{"message":…}}` (OpenAI, Anthropic, OpenRouter),
 * `{"error":"…"}` (Ollama), `{"message":…}` or `{"detail":"…"}` (Mistral).
 */
internal fun errorDetail(body: String, apiKey: String?): String? {
    val root = try {
        AiHttp.json.parseToJsonElement(body) as? JsonObject
    } catch (e: IllegalArgumentException) {
        null // kotlinx.serialization's parse error is an IllegalArgumentException
    } ?: return null
    val error = root["error"]
    val message = (error as? JsonObject)?.get("message").asText()
        ?: error.asText()
        ?: root["message"].asText()
        ?: root["detail"].asText()
        ?: return null
    return redact(message, apiKey).take(MAX_DETAIL)
}

/** Removes the key from a text, in case a server echoes it back. */
internal fun redact(text: String, apiKey: String?): String =
    if (apiKey.isNullOrBlank()) text else text.replace(apiKey, "***")

private fun kotlinx.serialization.json.JsonElement?.asText(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

/** Decodes [text] or throws INVALID_RESPONSE. */
internal inline fun <reified T> decodeResponse(text: String): T = try {
    AiHttp.json.decodeFromString<T>(text)
} catch (e: IllegalArgumentException) {
    throw AiException(AiException.Reason.INVALID_RESPONSE, cause = e)
}

private const val MAX_DETAIL = 300
