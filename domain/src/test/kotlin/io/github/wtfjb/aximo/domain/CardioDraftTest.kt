package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.cardio.CardioDraft
import io.github.wtfjb.aximo.domain.cardio.CardioFieldError
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.CardioSource
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardioDraftTest {

    private val t0 = Instant.fromEpochSeconds(1_790_000_000)

    @Test
    fun `activity and duration are required`() {
        assertEquals(
            setOf(CardioFieldError.ACTIVITY_MISSING, CardioFieldError.DURATION_MISSING),
            CardioDraft(startedAt = t0).validate(),
        )
        assertNull(CardioDraft(exerciseId = 1, startedAt = t0).toEntry())
    }

    @Test
    fun `optional fields may stay empty`() {
        val entry = CardioDraft(exerciseId = 1, startedAt = t0, durationSec = 1721, note = "  locker  ").toEntry()!!
        assertEquals(1721, entry.durationSec)
        assertNull(entry.distanceM)
        assertNull(entry.avgHeartRate)
        assertEquals("locker", entry.note)
    }

    @Test
    fun `editing keeps id, workout and source`() {
        val original = CardioEntry(
            id = 7,
            workoutId = 3,
            exerciseId = 1,
            startedAt = t0,
            durationSec = 600,
            source = CardioSource.HEALTH_CONNECT,
            externalId = "x",
        )
        val edited = CardioDraft.from(original).copy(durationSec = 900, distanceM = 2000.0).toEntry(original)!!
        assertEquals(original.copy(durationSec = 900, distanceM = 2000.0), edited)
    }

    @Test
    fun `parse distance in km`() {
        assertEquals(5200.0, CardioDraft.parseDistanceM("5,2")!!, 0.001)
        assertEquals(5200.0, CardioDraft.parseDistanceM("5.20")!!, 0.001)
        assertNull(CardioDraft.parseDistanceM("0"))
        assertNull(CardioDraft.parseDistanceM("-1"))
        assertNull(CardioDraft.parseDistanceM("abc"))
        assertEquals("5.2", CardioDraft.formatKm(5200.0))
    }

    @Test
    fun `parse heart rate and elevation`() {
        assertEquals(152, CardioDraft.parseHeartRate(" 152 "))
        assertNull(CardioDraft.parseHeartRate("5"))
        assertNull(CardioDraft.parseHeartRate("152,5"))
        assertEquals(0.0, CardioDraft.parseElevationM("0")!!, 0.0)
        assertEquals(85.0, CardioDraft.parseElevationM("85")!!, 0.0)
        assertNull(CardioDraft.parseElevationM("-10"))
    }
}
