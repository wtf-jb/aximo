package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiProviderFactory
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.ktor.client.HttpClient

/** Picks the adapter for a profile's kind; all share one HTTP client. */
class KtorAiProviderFactory(private val client: HttpClient) : AiProviderFactory {

    override fun create(profile: AiProviderProfile, apiKey: String?): AiProvider {
        val baseUrl = AiProfiles.normalizeBaseUrl(profile.baseUrl)
        return when (profile.kind) {
            AiProviderKind.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(client, baseUrl, profile.model, apiKey)
            AiProviderKind.ANTHROPIC -> AnthropicProvider(client, baseUrl, profile.model, apiKey)
        }
    }
}
