package io.github.wtfjb.aximo.ui.ai

import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory profiles and keys with the same rules as the Room repository. */
class FakeAiProfileRepository : AiProfileRepository {
    private val rows = MutableStateFlow<List<AiProviderProfile>>(emptyList())
    val keys = mutableMapOf<Long, String>()
    private var nextId = 1L

    override fun observeProfiles(): Flow<List<AiProviderProfile>> =
        rows.map { list -> list.map { it.copy(hasApiKey = it.id in keys) } }

    override suspend fun getProfile(id: Long): AiProviderProfile? =
        rows.value.firstOrNull { it.id == id }?.copy(hasApiKey = id in keys)

    override suspend fun saveProfile(profile: AiProviderProfile, key: ApiKeyChange): Long {
        val id = if (profile.id == 0L) nextId++ else profile.id
        when (key) {
            ApiKeyChange.Keep -> Unit
            ApiKeyChange.Remove -> keys.remove(id)
            is ApiKeyChange.Set -> keys[id] = key.value
        }
        val existing = rows.value.firstOrNull { it.id == id }
        val active = existing?.active ?: rows.value.isEmpty()
        val saved = profile.copy(id = id, active = active)
        rows.value = if (existing == null) rows.value + saved else rows.value.map { if (it.id == id) saved else it }
        return id
    }

    override suspend fun deleteProfile(id: Long) {
        keys.remove(id)
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun setActive(id: Long) {
        rows.value = rows.value.map { it.copy(active = it.id == id) }
    }

    override suspend fun apiKey(id: Long): String? = keys[id]

    companion object {
        fun profile(name: String, kind: AiProviderKind = AiProviderKind.OPENAI_COMPATIBLE) =
            AiProviderProfile(name = name, kind = kind, baseUrl = "http://nas:11434/v1", model = "llama3.2")
    }
}
