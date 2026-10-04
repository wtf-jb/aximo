package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.units.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightUnitTest {

    @Test
    fun kgIsUnchanged() {
        assertEquals(82.5, WeightUnit.KG.fromKg(82.5), 0.0)
        assertEquals(82.5, WeightUnit.KG.toKg(82.5), 0.0)
    }

    @Test
    fun convertsKgToLbs() {
        assertEquals(220.462, WeightUnit.LBS.fromKg(100.0), 0.001)
    }

    @Test
    fun convertsLbsToKg() {
        assertEquals(45.359237, WeightUnit.LBS.toKg(100.0), 1e-9)
    }

    @Test
    fun roundTripKeepsTheValue() {
        val kg = 83.7
        assertEquals(kg, WeightUnit.LBS.toKg(WeightUnit.LBS.fromKg(kg)), 1e-9)
    }
}
