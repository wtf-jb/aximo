package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.ai.AiKeyStore
import io.github.wtfjb.aximo.data.db.dao.AiProfileDao
import io.github.wtfjb.aximo.data.db.entity.AiProfileEntity
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Profiles in Room, keys in the [AiKeyStore]. */
class RoomAiProfileRepository(
    private val dao: AiProfileDao,
    private val keys: AiKeyStore,
) : AiProfileRepository {

    override fun observeProfiles(): Flow<List<AiProviderProfile>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    override suspend fun getProfile(id: Long): AiProviderProfile? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun saveProfile(profile: AiProviderProfile, key: ApiKeyChange): Long = withContext(Dispatchers.IO) {
        val id = if (profile.id == 0L) dao.insertNew(profile.toEntity()) else profile.id
        // The key is not in the table, so the row is written after the key:
        // that write makes the profile list emit again with the new key state.
        applyKey(id, key)
        val active = dao.getById(id)?.active ?: profile.active
        dao.update(profile.toEntity().copy(id = id, active = active))
        id
    }

    override suspend fun deleteProfile(id: Long) = withContext(Dispatchers.IO) {
        keys.remove(id)
        dao.delete(id)
    }

    override suspend fun setActive(id: Long) = withContext(Dispatchers.IO) { dao.setActive(id) }

    override suspend fun apiKey(id: Long): String? = withContext(Dispatchers.IO) { keys.get(id) }

    private fun applyKey(id: Long, change: ApiKeyChange) {
        when (change) {
            ApiKeyChange.Keep -> Unit
            ApiKeyChange.Remove -> keys.remove(id)
            is ApiKeyChange.Set -> keys.put(id, change.value)
        }
    }

    private fun AiProfileEntity.toDomain() = AiProviderProfile(
        id = id,
        name = name,
        kind = AiProviderKind.valueOf(kind),
        baseUrl = baseUrl,
        model = model,
        active = active,
        hasApiKey = keys.contains(id),
    )

    private fun AiProviderProfile.toEntity() = AiProfileEntity(
        id = id,
        name = name,
        kind = kind.name,
        baseUrl = baseUrl,
        model = model,
        active = active,
    )
}
