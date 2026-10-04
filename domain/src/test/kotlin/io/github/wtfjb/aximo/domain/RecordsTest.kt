package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.stats.RecordType
import io.github.wtfjb.aximo.domain.stats.Records
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordsTest {

    private val done = Instant.fromEpochSeconds(1_790_000_000)
    private fun s(weight: Double, reps: Int, session: Long = 1, type: SetType = SetType.WORKING) =
        SetEntry(workoutExerciseId = session, position = 0, weightKg = weight, reps = reps, setType = type, completedAt = done)

    @Test
    fun epleyEstimate() {
        assertEquals(100.0, Records.e1rm(100.0, 1)!!, 1e-9)
        assertEquals(104.5, Records.e1rm(95.0, 3)!!, 1e-9)
        assertEquals(106.666, Records.e1rm(80.0, 10)!!, 0.001)
        assertNull(Records.e1rm(60.0, 13))
        assertNull(Records.e1rm(60.0, 0))
    }

    @Test
    fun volumeSkipsWarmUps() {
        assertEquals(1280.0, Records.volume(listOf(s(40.0, 10, type = SetType.WARM_UP), s(80.0, 8), s(80.0, 8))), 0.0)
    }

    @Test
    fun firstSessionHasNoRecord() {
        assertNull(Records.newRecord(listOf(s(80.0, 8)), emptyList()))
    }

    @Test
    fun higherE1rmIsTheRecord() {
        val record = Records.newRecord(listOf(s(85.0, 6)), listOf(s(80.0, 6)))!!
        assertEquals(RecordType.E1RM, record.type)
        assertEquals(102.0, record.value, 1e-9)
        assertEquals(96.0, record.previous, 1e-9)
    }

    @Test
    fun heavierSingleAboveTwelveRepsHistoryIsAWeightRecord() {
        // History only has 15-rep sets (no e1RM), so the comparison falls to plain weight.
        val record = Records.newRecord(listOf(s(50.0, 15)), listOf(s(45.0, 15)))!!
        assertEquals(RecordType.WEIGHT, record.type)
    }

    @Test
    fun moreRepsAtTheSameWeight() {
        val record = Records.newRecord(listOf(s(12.5, 14)), listOf(s(12.5, 13)))!!
        assertEquals(RecordType.REPS_AT_WEIGHT, record.type)
        assertEquals(14.0, record.value, 0.0)
        assertEquals(12.5, record.weightKg, 0.0)
    }

    @Test
    fun moreVolumeWithoutOtherRecords() {
        val history = listOf(s(80.0, 8, session = 1), s(80.0, 8, session = 1), s(80.0, 8, session = 2))
        val record = Records.newRecord(listOf(s(80.0, 8, session = 3), s(80.0, 8, session = 3), s(80.0, 8, session = 3)), history)!!
        assertEquals(RecordType.VOLUME, record.type)
        assertEquals(1920.0, record.value, 0.0)
        assertEquals(1280.0, record.previous, 0.0)
    }

    @Test
    fun noRecordWhenNothingImproved() {
        assertNull(Records.newRecord(listOf(s(80.0, 8)), listOf(s(80.0, 8), s(80.0, 8))))
    }
}
