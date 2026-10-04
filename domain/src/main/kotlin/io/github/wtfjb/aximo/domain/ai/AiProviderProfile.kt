package io.github.wtfjb.aximo.domain.ai

/**
 * A provider profile (B-01): name, kind, base URL and model. The API key is not
 * part of it; it is stored encrypted on its own and only [hasApiKey] says
 * whether there is one. Exactly one profile is active (see [AiProfiles.active]).
 */
data class AiProviderProfile(
    val id: Long = 0,
    val name: String,
    val kind: AiProviderKind,
    val baseUrl: String,
    val model: String,
    val active: Boolean = false,
    val hasApiKey: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Profile name must not be blank" }
        require(AiProfiles.isValidBaseUrl(baseUrl)) { "Invalid base URL" }
        require(model.isNotBlank()) { "Model must not be blank" }
    }
}

/** What to do with the stored API key when a profile is saved. */
sealed interface ApiKeyChange {
    data object Keep : ApiKeyChange
    data object Remove : ApiKeyChange

    class Set(val value: String) : ApiKeyChange {
        init {
            require(value.isNotBlank()) { "API key must not be blank" }
        }

        // Never print the key, not even in a test failure.
        override fun toString(): String = "Set(***)"
    }
}

object AiProfiles {
    const val ANTHROPIC_BASE_URL = "https://api.anthropic.com"

    /** Shown as an example in the empty model field; a model ID, not a UI text. */
    const val ANTHROPIC_MODEL_EXAMPLE = "claude-sonnet-5-5"

    private val URL = Regex("^https?://[^\\s/?#]+(/[^\\s?#]*)?$", RegexOption.IGNORE_CASE)

    /** http(s), a host, optionally a path; no query. A trailing slash is fine. */
    fun isValidBaseUrl(url: String): Boolean = URL.matches(url.trim())

    /** Trims and drops trailing slashes, so the adapters can append paths. */
    fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/')

    /** Plain http: the key and the training data would go unencrypted over the network. */
    fun isCleartext(url: String): Boolean = url.trim().startsWith("http://", ignoreCase = true)

    /** The flagged profile, or the first one if none is flagged (e.g. after the active one was deleted). */
    fun active(profiles: List<AiProviderProfile>): AiProviderProfile? =
        profiles.firstOrNull { it.active } ?: profiles.firstOrNull()

    /** AI features show only when at least one profile exists. */
    fun isAvailable(profiles: List<AiProviderProfile>): Boolean = profiles.isNotEmpty()
}
