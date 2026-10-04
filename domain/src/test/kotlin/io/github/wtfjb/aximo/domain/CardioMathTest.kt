package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.cardio.CardioMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardioMathTest {

    @Test
    fun `pace per km from mockup values`() {
        // 28:41 over 5.20 km → 5:31 /km.
        val pace = CardioMath.paceSecondsPerKm(28 * 60 + 41, 5200.0)!!
        assertEquals("5:31", CardioMath.formatPace(pace))
    }

    @Test
    fun `speed from mockup values`() {
        val speed = CardioMath.speedKmh(28 * 60 + 41, 5200.0)!!
        assertEquals(10.88, speed, 0.01)
    }

    @Test
    fun `pace per 500 m is half the pace per km`() {
        // 2000 m in 8:00 → 2:00 /500 m.
        assertEquals(120.0, CardioMath.paceSecondsPer500m(480, 2000.0)!!, 0.001)
    }

    @Test
    fun `no distance means no pace or speed`() {
        assertNull(CardioMath.paceSecondsPerKm(1800, null))
        assertNull(CardioMath.paceSecondsPerKm(1800, 0.0))
        assertNull(CardioMath.speedKmh(1800, null))
        assertNull(CardioMath.speedKmh(0, 5000.0))
    }

    @Test
    fun `pace rounding carries into the next minute`() {
        assertEquals("6:00", CardioMath.formatPace(359.6))
    }

    @Test
    fun `durations below and above one hour`() {
        assertEquals("28:41", CardioMath.formatDuration(1721))
        assertEquals("1:05:30", CardioMath.formatDuration(3930))
        assertEquals("0:00", CardioMath.formatDuration(-5))
    }

    @Test
    fun `split and join duration fields`() {
        assertEquals(Triple(1, 5, 30), CardioMath.split(3930))
        assertEquals(3930, CardioMath.durationOf("1", "5", "30"))
        assertEquals(1721, CardioMath.durationOf("", "28", "41"))
        assertEquals(5400, CardioMath.durationOf("", "90", ""))
    }

    @Test
    fun `invalid or zero duration fields`() {
        assertNull(CardioMath.durationOf("", "", ""))
        assertNull(CardioMath.durationOf("0", "0", "0"))
        assertNull(CardioMath.durationOf("", "-3", ""))
        assertNull(CardioMath.durationOf("", "2,5", ""))
    }
}
