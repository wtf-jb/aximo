package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.pullUp
import io.github.wtfjb.aximo.domain.StatsTestData.set
import io.github.wtfjb.aximo.domain.StatsTestData.workout
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.stats.MuscleVolume
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleVolumeTest {

    private val day = at(LocalDate(2026, 10, 5))

    @Test
    fun `primary counts one, secondary half`() {
        assertEquals(1.0, MuscleVolume.weight(bench, BodyRegion.CHEST), 0.0)
        assertEquals(0.5, MuscleVolume.weight(bench, BodyRegion.ARMS), 0.0)
        assertEquals(0.5, MuscleVolume.weight(bench, BodyRegion.SHOULDERS), 0.0)
        assertEquals(0.0, MuscleVolume.weight(bench, BodyRegion.LEGS), 0.0)
    }

    @Test
    fun `several primary muscles of one region count once`() {
        assertEquals(1.0, MuscleVolume.weight(pullUp, BodyRegion.BACK), 0.0)
    }

    @Test
    fun `primary wins over secondary in the same region`() {
        val row = bench.copy(primaryMuscles = setOf(MuscleGroup.LATS), secondaryMuscles = setOf(MuscleGroup.LOWER_BACK))
        assertEquals(1.0, MuscleVolume.weight(row, BodyRegion.BACK), 0.0)
    }

    @Test
    fun `sets per region skip warm-ups, open sets and running workouts`() {
        val totals = MuscleVolume.setsPerRegion(
            listOf(
                workout(
                    1,
                    day,
                    bench to listOf(set(40.0, 10, SetType.WARM_UP), set(80.0, 8), set(80.0, 8), set(80.0, 8, done = false)),
                    pullUp to listOf(set(0.0, 8), set(0.0, 8), set(0.0, 7, SetType.DROP)),
                ),
                workout(2, day, bench to listOf(set(80.0, 8)), finished = false),
            ),
        )
        assertEquals(2.0, totals.getValue(BodyRegion.CHEST), 0.0)
        assertEquals(3.0, totals.getValue(BodyRegion.BACK), 0.0)
        assertEquals(1.0 + 1.5, totals.getValue(BodyRegion.ARMS), 0.0)
        assertEquals(1.0, totals.getValue(BodyRegion.SHOULDERS), 0.0)
        assertEquals(0.0, totals.getValue(BodyRegion.LEGS), 0.0)
        assertEquals(MuscleVolume.regions.toSet(), totals.keys)
    }

    @Test
    fun `per week divides by the weeks`() {
        val perWeek = MuscleVolume.perWeek(mapOf(BodyRegion.CHEST to 40.0), 4)
        assertEquals(10.0, perWeek.getValue(BodyRegion.CHEST), 0.0)
        assertTrue(MuscleVolume.belowTarget(9.5))
        assertFalse(MuscleVolume.belowTarget(10.0))
    }
}
