package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.set
import io.github.wtfjb.aximo.domain.StatsTestData.workout
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.stats.WorkoutComparison
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutComparisonTest {

    private val before = workout(1, at(LocalDate(2026, 9, 28)), bench to listOf(set(80.0, 10), set(80.0, 10)))
    private val now = workout(2, at(LocalDate(2026, 10, 1)), bench to listOf(set(40.0, 10, SetType.WARM_UP), set(82.5, 10), set(83.5, 10)))

    @Test
    fun `volume change ignores warm-ups`() {
        assertEquals(0.0375, WorkoutComparison.volumeChange(now, before)!!, 1e-9)
    }

    @Test
    fun `no previous workout, no change`() {
        assertNull(WorkoutComparison.volumeChange(now, null))
        assertNull(WorkoutComparison.volumeChange(now, workout(1, at(LocalDate(2026, 9, 28)))))
    }
}
