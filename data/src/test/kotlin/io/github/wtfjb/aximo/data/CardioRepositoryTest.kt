package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.repository.RoomCardioRepository
import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.domain.model.CardioEntry
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardioRepositoryTest : DatabaseTest() {

    private lateinit var cardio: RoomCardioRepository
    private var runId = 0L
    private val t0 = Instant.fromEpochSeconds(1_790_000_000)

    @Before
    fun setUp() = runTest {
        cardio = RoomCardioRepository(db.cardioDao())
        runId = RoomExerciseRepository(db.exerciseDao()).saveExercise(running())
    }

    private fun entry(start: Instant) = CardioEntry(
        exerciseId = runId,
        startedAt = start,
        durationSec = 1721,
        distanceM = 5200.0,
        avgHeartRate = 152,
        note = "locker",
    )

    @Test
    fun savedEntryCanBeReadUpdatedAndDeleted() = runTest {
        val id = cardio.saveEntry(entry(t0))
        val stored = cardio.getEntry(id)!!
        assertEquals(entry(t0).copy(id = id), stored)

        assertEquals(id, cardio.saveEntry(stored.copy(durationSec = 1800, avgHeartRate = null)))
        assertEquals(1800, cardio.getEntry(id)!!.durationSec)
        assertNull(cardio.getEntry(id)!!.avgHeartRate)

        cardio.deleteEntry(id)
        assertNull(cardio.getEntry(id))
    }

    @Test
    fun recentEntriesAreNewestFirstAndLimited() = runTest {
        cardio.saveEntry(entry(t0))
        cardio.saveEntry(entry(t0 + 2.days))
        cardio.saveEntry(entry(t0 + 1.days))

        val recent = cardio.observeRecent(2).first()

        assertEquals(listOf(t0 + 2.days, t0 + 1.days), recent.map { it.startedAt })
    }
}
