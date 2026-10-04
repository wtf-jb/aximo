package io.github.wtfjb.aximo.domain.calories

import io.github.wtfjb.aximo.domain.cardio.DefaultActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieEstimateTest {

    @Test
    fun strengthHourAt80KgIs200() {
        // (3.5 − 1) × 80 kg × 1 h
        assertEquals(200, CalorieEstimate.strength(3600, 80.0))
    }

    @Test
    fun strengthRoundsDownToTens() {
        // 2.5 × 75 × 0.75 h = 140.6
        assertEquals(140, CalorieEstimate.strength(45 * 60, 75.0))
    }

    @Test
    fun strengthCountsAtMostThreeHours() {
        assertEquals(CalorieEstimate.strength(3 * 3600, 80.0), CalorieEstimate.strength(10 * 3600, 80.0))
    }

    @Test
    fun zeroOrNegativeDurationIsZero() {
        assertEquals(0, CalorieEstimate.strength(0, 80.0))
        assertEquals(0, CalorieEstimate.strength(-60, 80.0))
    }

    @Test
    fun runningWithDistanceCountsPerKilometre() {
        // 0.9 × 80 kg × 10 km, same for slow and fast runs
        assertEquals(720, CalorieEstimate.cardio(DefaultActivity.RUNNING, 50 * 60, 10_000.0, 80.0))
        assertEquals(720, CalorieEstimate.cardio(DefaultActivity.RUNNING, 70 * 60, 10_000.0, 80.0))
    }

    @Test
    fun slowRunningCountsAsWalking() {
        // 5 km in 60 min = 5 km/h → 0.5 × 80 × 5
        assertEquals(200, CalorieEstimate.cardio(DefaultActivity.RUNNING, 3600, 5_000.0, 80.0))
    }

    @Test
    fun runningWithoutDistanceUsesTime() {
        // (7 − 1) × 80 × 0.5 h
        assertEquals(240, CalorieEstimate.cardio(DefaultActivity.RUNNING, 30 * 60, null, 80.0))
        assertEquals(240, CalorieEstimate.cardio(DefaultActivity.RUNNING, 30 * 60, 0.0, 80.0))
    }

    @Test
    fun cyclingDependsOnSpeed() {
        // 1 h at 15 km/h: (4 − 1) × 80
        assertEquals(240, CalorieEstimate.cardio(DefaultActivity.CYCLING, 3600, 15_000.0, 80.0))
        // 1 h at 25 km/h: (9 − 1) × 80
        assertEquals(640, CalorieEstimate.cardio(DefaultActivity.CYCLING, 3600, 25_000.0, 80.0))
        // without distance: (5 − 1) × 80
        assertEquals(320, CalorieEstimate.cardio(DefaultActivity.CYCLING, 3600, null, 80.0))
    }

    @Test
    fun rowingDependsOnPace() {
        // 30 min, 6000 m = 2:30 / 500 m → (7 − 1) × 80 × 0.5
        assertEquals(240, CalorieEstimate.cardio(DefaultActivity.ROWING, 30 * 60, 6_000.0, 80.0))
        // 30 min, 5000 m = 3:00 / 500 m → (4.8 − 1) × 80 × 0.5 = 152
        assertEquals(150, CalorieEstimate.cardio(DefaultActivity.ROWING, 30 * 60, 5_000.0, 80.0))
        // 30 min, 7500 m = 2:00 / 500 m → (8.5 − 1) × 80 × 0.5
        assertEquals(300, CalorieEstimate.cardio(DefaultActivity.ROWING, 30 * 60, 7_500.0, 80.0))
    }

    @Test
    fun ownAndOtherActivitiesAreModerate() {
        // (4 − 1) × 80 × 1 h
        assertEquals(240, CalorieEstimate.cardio(null, 3600, null, 80.0))
        assertEquals(240, CalorieEstimate.cardio(DefaultActivity.OTHER, 3600, 12_000.0, 80.0))
    }

    @Test
    fun scalesWithBodyWeight() {
        assertEquals(250, CalorieEstimate.strength(3600, 100.0))
        assertEquals(150, CalorieEstimate.strength(3600, 60.0))
    }
}
