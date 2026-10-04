package io.github.wtfjb.aximo.domain.ai

/** Problems in the profile form. */
enum class AiProfileFieldError { NAME_MISSING, URL_INVALID, MODEL_MISSING, KEY_MISSING }

/**
 * The profile form (B-01) as typed. [apiKey] is only what the user typed now;
 * [hasStoredKey] says whether a key is already saved. An empty [apiKey] keeps the
 * stored key, unless [removeKey] is set.
 */
data class AiProfileDraft(
    val id: Long = 0,
    val name: String = "",
    val kind: AiProviderKind = AiProviderKind.OPENAI_COMPATIBLE,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val hasStoredKey: Boolean = false,
    val removeKey: Boolean = false,
    val active: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L

    /** Plain http, show a warning. */
    val isCleartext: Boolean get() = AiProfiles.isCleartext(baseUrl)

    /** A key will be there after saving. */
    val willHaveKey: Boolean get() = apiKey.isNotBlank() || (hasStoredKey && !removeKey)

    /**
     * Enough to ask for the model list: a valid URL and a key (typed or stored).
     * Servers without a key (local Ollama) get no list; the model is typed there.
     */
    val canListModels: Boolean
        get() = AiProfiles.isValidBaseUrl(baseUrl) && willHaveKey

    /**
     * Switching to Anthropic fills in its URL if the field is empty; switching
     * away clears it again if it still is Anthropic's.
     */
    fun withKind(newKind: AiProviderKind): AiProfileDraft {
        val url = when {
            newKind == AiProviderKind.ANTHROPIC && baseUrl.isBlank() -> AiProfiles.ANTHROPIC_BASE_URL
            newKind != AiProviderKind.ANTHROPIC && AiProfiles.normalizeBaseUrl(baseUrl) == AiProfiles.ANTHROPIC_BASE_URL -> ""
            else -> baseUrl
        }
        return copy(kind = newKind, baseUrl = url)
    }

    fun errors(): Set<AiProfileFieldError> = buildSet {
        if (name.isBlank()) add(AiProfileFieldError.NAME_MISSING)
        if (!AiProfiles.isValidBaseUrl(baseUrl)) add(AiProfileFieldError.URL_INVALID)
        if (model.isBlank()) add(AiProfileFieldError.MODEL_MISSING)
        // Anthropic always needs a key; OpenAI-compatible servers like Ollama work without.
        if (kind == AiProviderKind.ANTHROPIC && !willHaveKey) add(AiProfileFieldError.KEY_MISSING)
    }

    /** The profile to save, or null if the form has errors. */
    fun toProfile(): AiProviderProfile? {
        if (errors().isNotEmpty()) return null
        return AiProviderProfile(
            id = id,
            name = name.trim(),
            kind = kind,
            baseUrl = AiProfiles.normalizeBaseUrl(baseUrl),
            model = model.trim(),
            active = active,
            hasApiKey = willHaveKey,
        )
    }

    /** What to do with the stored key on save. */
    fun keyChange(): ApiKeyChange = when {
        apiKey.isNotBlank() -> ApiKeyChange.Set(apiKey.trim())
        removeKey -> ApiKeyChange.Remove
        else -> ApiKeyChange.Keep
    }

    // The typed key must never end up in a log or crash report.
    override fun toString(): String =
        "AiProfileDraft(id=$id, name=$name, kind=$kind, baseUrl=$baseUrl, model=$model, apiKey=***, " +
            "hasStoredKey=$hasStoredKey, removeKey=$removeKey, active=$active)"

    companion object {
        fun from(profile: AiProviderProfile) = AiProfileDraft(
            id = profile.id,
            name = profile.name,
            kind = profile.kind,
            baseUrl = profile.baseUrl,
            model = profile.model,
            hasStoredKey = profile.hasApiKey,
            active = profile.active,
        )
    }
}
