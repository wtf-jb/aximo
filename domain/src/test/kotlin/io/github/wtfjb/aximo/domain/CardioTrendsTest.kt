package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.stats.CardioMetric
import io.github.wtfjb.aximo.domain.stats.CardioTrends
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardioTrendsTest {

    private val run = Exercise(id = 1, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)
    private val row = Exercise(id = 2, name = "Rudern", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)

    private val older = CardioEntry(id = 1, exerciseId = run.id, startedAt = at(LocalDate(2026, 9, 29)), durationSec = 1800, distanceM = 5000.0, avgHeartRate = 150)
    private val newer = CardioEntry(id = 2, exerciseId = run.id, startedAt = at(LocalDate(2026, 10, 2)), durationSec = 1500, distanceM = null)
    private val rowing = CardioEntry(id = 3, exerciseId = row.id, startedAt = at(LocalDate(2026, 10, 1)), durationSec = 1200, distanceM = 5000.0, avgHeartRate = 141)

    @Test
    fun `activities most recent first`() {
        assertEquals(listOf(run.id, row.id), CardioTrends.activities(listOf(older, rowing, newer), listOf(run, row)).map { it.id })
    }

    @Test
    fun `pace points follow the pace style and skip entries without distance`() {
        val perKm = CardioTrends.points(listOf(newer, older), CardioMetric.PACE, PaceStyle.PER_KM)
        assertEquals(listOf(1L), perKm.map { it.entryId })
        assertEquals(360.0, perKm.single().value, 1e-9)
        assertEquals(120.0, CardioTrends.value(rowing, CardioMetric.PACE, PaceStyle.PER_500M)!!, 1e-9)
        assertEquals(15.0, CardioTrends.value(rowing, CardioMetric.PACE, PaceStyle.SPEED)!!, 1e-9)
    }

    @Test
    fun `other metrics`() {
        assertEquals(5000.0, CardioTrends.value(older, CardioMetric.DISTANCE, PaceStyle.PER_KM)!!, 0.0)
        assertNull(CardioTrends.value(newer, CardioMetric.DISTANCE, PaceStyle.PER_KM))
        assertEquals(1500.0, CardioTrends.value(newer, CardioMetric.DURATION, PaceStyle.PER_KM)!!, 0.0)
        assertEquals(150.0, CardioTrends.value(older, CardioMetric.HEART_RATE, PaceStyle.PER_KM)!!, 0.0)
        assertNull(CardioTrends.value(newer, CardioMetric.HEART_RATE, PaceStyle.PER_KM))
        val durations = CardioTrends.points(listOf(newer, older), CardioMetric.DURATION, PaceStyle.PER_KM)
        assertEquals(listOf(1L, 2L), durations.map { it.entryId })
    }

    @Test
    fun summary() {
        val summary = CardioTrends.summary(listOf(older, newer, rowing))
        assertEquals(3, summary.sessions)
        assertEquals(10000.0, summary.distanceM, 0.0)
        assertEquals(4500L, summary.durationSec)
        assertEquals(146, summary.avgHeartRate) // 145.5 rounded
        assertNull(CardioTrends.summary(listOf(newer)).avgHeartRate)
    }
}
