package io.github.wtfjb.aximo.data.ai

/** Encrypted storage for API keys, one per profile id. */
interface AiKeyStore {
    fun get(profileId: Long): String?
    fun put(profileId: Long, key: String)
    fun remove(profileId: Long)
    fun contains(profileId: Long): Boolean
}
