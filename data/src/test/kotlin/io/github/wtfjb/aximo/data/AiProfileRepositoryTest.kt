package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.ai.AiKeyStore
import io.github.wtfjb.aximo.data.repository.RoomAiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AiProfileRepositoryTest : DatabaseTest() {

    private val keys = FakeKeyStore()
    private lateinit var repo: RoomAiProfileRepository

    @Before
    fun setUp() {
        repo = RoomAiProfileRepository(db.aiProfileDao(), keys)
    }

    private fun profile(name: String, kind: AiProviderKind = AiProviderKind.OPENAI_COMPATIBLE) =
        AiProviderProfile(name = name, kind = kind, baseUrl = "https://api.example.com/v1", model = "m")

    @Test
    fun firstProfileBecomesActive() = runTest {
        val first = repo.saveProfile(profile("A"), ApiKeyChange.Keep)
        val second = repo.saveProfile(profile("B"), ApiKeyChange.Keep)

        val all = repo.observeProfiles().first()
        assertEquals(listOf("A", "B"), all.map { it.name })
        assertTrue(all.first { it.id == first }.active)
        assertFalse(all.first { it.id == second }.active)

        repo.setActive(second)
        assertEquals(second, AiProfiles.active(repo.observeProfiles().first())?.id)
        assertEquals(1, repo.observeProfiles().first().count { it.active })
    }

    @Test
    fun keyIsStoredSeparatelyAndCanBeKeptReplacedRemoved() = runTest {
        val id = repo.saveProfile(profile("Claude", AiProviderKind.ANTHROPIC), ApiKeyChange.Set("sk-1"))
        assertEquals("sk-1", repo.apiKey(id))
        assertTrue(repo.getProfile(id)!!.hasApiKey)
        assertEquals(AiProviderKind.ANTHROPIC, repo.getProfile(id)!!.kind)

        repo.saveProfile(repo.getProfile(id)!!.copy(model = "m2"), ApiKeyChange.Keep)
        assertEquals("sk-1", repo.apiKey(id))
        assertEquals("m2", repo.getProfile(id)!!.model)

        repo.saveProfile(repo.getProfile(id)!!, ApiKeyChange.Set("sk-2"))
        assertEquals("sk-2", repo.apiKey(id))

        repo.saveProfile(repo.getProfile(id)!!, ApiKeyChange.Remove)
        assertNull(repo.apiKey(id))
        assertFalse(repo.observeProfiles().first().single().hasApiKey)
    }

    @Test
    fun editingKeepsActiveFlag() = runTest {
        val id = repo.saveProfile(profile("A"), ApiKeyChange.Keep)
        // The form may hold a stale copy; saving must not deactivate the profile.
        repo.saveProfile(profile("A2").copy(id = id, active = false), ApiKeyChange.Keep)

        assertTrue(repo.getProfile(id)!!.active)
    }

    @Test
    fun deleteRemovesKeyAndNextProfileTakesOver() = runTest {
        val first = repo.saveProfile(profile("A"), ApiKeyChange.Set("sk"))
        val second = repo.saveProfile(profile("B"), ApiKeyChange.Keep)

        repo.deleteProfile(first)

        assertNull(repo.getProfile(first))
        assertFalse(keys.contains(first))
        assertEquals(second, AiProfiles.active(repo.observeProfiles().first())?.id)
    }
}

/** In-memory stand-in: Robolectric has no Android Keystore. */
class FakeKeyStore : AiKeyStore {
    private val values = mutableMapOf<Long, String>()
    override fun get(profileId: Long): String? = values[profileId]
    override fun put(profileId: Long, key: String) { values[profileId] = key }
    override fun remove(profileId: Long) { values.remove(profileId) }
    override fun contains(profileId: Long): Boolean = profileId in values
}
