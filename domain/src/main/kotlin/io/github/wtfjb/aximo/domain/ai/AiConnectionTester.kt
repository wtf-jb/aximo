package io.github.wtfjb.aximo.domain.ai

/** Result of "Verbindung testen". */
sealed interface AiConnectionResult {
    data object Success : AiConnectionResult
    data class Failure(val error: AiException) : AiConnectionResult
}

/**
 * Sends a tiny request with the profile's settings (B-01). No training data is
 * sent, only a fixed prompt.
 */
class AiConnectionTester(private val factory: AiProviderFactory) {

    suspend fun test(profile: AiProviderProfile, apiKey: String?): AiConnectionResult = try {
        factory.create(profile, apiKey?.takeIf { it.isNotBlank() }).complete(REQUEST)
        AiConnectionResult.Success
    } catch (e: AiException) {
        AiConnectionResult.Failure(e)
    }

    companion object {
        /** Versioned with the code like every prompt (requirements, KI-Prinzipien). */
        val REQUEST = AiRequest(messages = listOf(AiMessage(AiMessage.Role.USER, "Reply with the single word OK.")))
    }
}
