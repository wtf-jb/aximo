package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiModelLister
import io.github.wtfjb.aximo.domain.ai.AiModels
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable

/**
 * The models a provider offers. OpenAI-compatible: `GET {baseUrl}/models`
 * (also Ollama, Mistral, OpenRouter). Anthropic: `GET {baseUrl}/v1/models`.
 * Both answer `{"data": [{"id": …}, …]}`.
 */
class KtorModelLister(private val client: HttpClient) : AiModelLister {

    override suspend fun listModels(kind: AiProviderKind, baseUrl: String, apiKey: String?): List<String> {
        val base = AiProfiles.normalizeBaseUrl(baseUrl)
        val key = apiKey?.takeIf { it.isNotBlank() }
        val text = when (kind) {
            AiProviderKind.OPENAI_COMPATIBLE -> {
                val headers = if (key == null) emptyMap() else mapOf("Authorization" to "Bearer $key")
                client.getJson("$base/models", headers, key)
            }
            AiProviderKind.ANTHROPIC -> {
                val headers = buildMap {
                    put("anthropic-version", AnthropicProvider.API_VERSION)
                    if (key != null) put("x-api-key", key)
                }
                val url = if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"
                // 1000 is the largest page; Anthropic has far fewer models.
                client.getJson("$url?limit=$ANTHROPIC_PAGE_SIZE", headers, key)
            }
        }
        val data = decodeResponse<ModelsResponse>(text).data ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        return AiModels.clean(data.mapNotNull { it.id })
    }

    @Serializable
    internal data class ModelsResponse(val data: List<Model>? = null)

    @Serializable
    internal data class Model(val id: String? = null)

    private companion object {
        const val ANTHROPIC_PAGE_SIZE = 1000
    }
}
