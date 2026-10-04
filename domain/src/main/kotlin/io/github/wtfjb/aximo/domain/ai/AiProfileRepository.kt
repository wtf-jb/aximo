package io.github.wtfjb.aximo.domain.ai

import kotlinx.coroutines.flow.Flow

/** Stores provider profiles and their API keys (B-01). */
interface AiProfileRepository {
    /** All profiles, oldest first. */
    fun observeProfiles(): Flow<List<AiProviderProfile>>

    suspend fun getProfile(id: Long): AiProviderProfile?

    /**
     * Inserts (id 0) or updates a profile and applies [key]. The first profile
     * becomes active. Returns the id.
     */
    suspend fun saveProfile(profile: AiProviderProfile, key: ApiKeyChange): Long

    /** Deletes the profile and its key. */
    suspend fun deleteProfile(id: Long)

    /** Makes [id] the only active profile. */
    suspend fun setActive(id: Long)

    /** The decrypted key, or null if there is none or it can no longer be decrypted. */
    suspend fun apiKey(id: Long): String?
}
