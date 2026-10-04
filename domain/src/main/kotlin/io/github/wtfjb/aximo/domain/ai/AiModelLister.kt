package io.github.wtfjb.aximo.domain.ai

/**
 * Asks a provider which models it offers, for the model dropdown in the profile
 * form. Sends only the API key, no training data. Throws [AiException].
 */
fun interface AiModelLister {
    suspend fun listModels(kind: AiProviderKind, baseUrl: String, apiKey: String?): List<String>
}

object AiModels {
    /** Sorted case-insensitively, without blanks and duplicates. */
    fun clean(ids: List<String>): List<String> =
        ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
}
