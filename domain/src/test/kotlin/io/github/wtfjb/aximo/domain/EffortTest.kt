package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.workout.Effort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EffortTest {

    private val set = SetEntry(workoutExerciseId = 1, position = 0, weightKg = 80.0, reps = 8)

    @Test
    fun rirComesFromRpeWhenLoggedThatWay() {
        assertEquals(2, Effort.rir(set.copy(rpe = 8.0)))
        assertEquals(1, Effort.rir(set.copy(rpe = 8.5)))
        assertEquals(0, Effort.rir(set.copy(rpe = 10.0)))
        assertEquals(3, Effort.rir(set.copy(rir = 3)))
        assertNull(Effort.rir(set))
    }

    @Test
    fun rpeComesFromRirWhenLoggedThatWay() {
        assertEquals(8.0, Effort.rpe(set.copy(rir = 2))!!, 0.0)
        assertEquals(1.0, Effort.rpe(set.copy(rir = 12))!!, 0.0)
        assertEquals(7.5, Effort.rpe(set.copy(rpe = 7.5))!!, 0.0)
        assertNull(Effort.rpe(set))
    }

    @Test
    fun rpeInputIsCheckedAndRoundedToHalfPoints() {
        assertEquals(8.5, Effort.parseRpe("8,5")!!, 0.0)
        assertEquals(8.0, Effort.parseRpe(" 8 ")!!, 0.0)
        assertEquals(9.5, Effort.parseRpe("9.4")!!, 0.0)
        assertNull(Effort.parseRpe("0.5"))
        assertNull(Effort.parseRpe("11"))
        assertNull(Effort.parseRpe("hart"))
    }

    @Test
    fun onlyOneScaleIsStored() {
        val rpe = Effort.withRpe(set.copy(rir = 2), 9.0)
        assertEquals(9.0, rpe.rpe!!, 0.0)
        assertNull(rpe.rir)
        val rir = Effort.withRir(rpe, 1)
        assertEquals(1, rir.rir)
        assertNull(rir.rpe)
    }
}
