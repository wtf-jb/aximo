package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.repository.RoomAiReviewRepository
import io.github.wtfjb.aximo.domain.review.AiReview
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionReason
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class AiReviewRepositoryTest : DatabaseTest() {

    private lateinit var repo: RoomAiReviewRepository

    @Before
    fun setUp() {
        repo = RoomAiReviewRepository(db.aiReviewDao())
    }

    private val changes = listOf(
        SuggestionChange.SetCount(7, 1, 3, 4),
        SuggestionChange.RepRange(7, 2, 6, 8, 8, 10),
        SuggestionChange.TargetRir(7, 1, null, 2),
        SuggestionChange.AddExercise(8, 5, 3, 10, 12, null),
        SuggestionChange.RemoveExercise(8, 6),
    )

    private fun review(at: Long, summary: String) = AiReview(
        createdAt = Instant.fromEpochSeconds(at),
        weeks = 6,
        summary = summary,
        suggestions = changes.map { AiSuggestion(change = it, rationale = "r ${it::class.simpleName}") },
        droppedSuggestions = 1,
    )

    @Test
    fun reasonIsStoredAndOptional() = runTest {
        repo.saveReview(
            AiReview(
                createdAt = Instant.fromEpochSeconds(1),
                weeks = 6,
                summary = "s",
                suggestions = listOf(
                    AiSuggestion(change = changes[0], rationale = "a", reason = SuggestionReason.VOLUME_LOW),
                    AiSuggestion(change = changes[1], rationale = "b"),
                ),
            ),
        )

        val stored = repo.observeLatest().first()!!.suggestions
        assertEquals(listOf(SuggestionReason.VOLUME_LOW, null), stored.map { it.reason })
        assertEquals(SuggestionReason.VOLUME_LOW, repo.getSuggestion(stored[0].id)!!.reason)
    }

    @Test
    fun roundTripAndLatest() = runTest {
        assertNull(repo.observeLatest().first())

        repo.saveReview(review(1_000, "old"))
        val id = repo.saveReview(review(2_000, "new"))

        val latest = repo.observeLatest().first()!!
        assertEquals(id, latest.id)
        assertEquals("new", latest.summary)
        assertEquals(1, latest.droppedSuggestions)
        assertEquals(changes, latest.suggestions.map { it.change })
        assertEquals(SuggestionStatus.OPEN, latest.suggestions.first().status)
        assertEquals("r SetCount", latest.suggestions.first().rationale)
    }

    @Test
    fun statusChanges() = runTest {
        repo.saveReview(review(1_000, "s"))
        val suggestion = repo.observeLatest().first()!!.suggestions[1]

        repo.setStatus(suggestion.id, SuggestionStatus.APPLIED)

        assertEquals(SuggestionStatus.APPLIED, repo.getSuggestion(suggestion.id)!!.status)
        assertEquals(SuggestionStatus.APPLIED, repo.observeLatest().first()!!.suggestions[1].status)
    }
}
