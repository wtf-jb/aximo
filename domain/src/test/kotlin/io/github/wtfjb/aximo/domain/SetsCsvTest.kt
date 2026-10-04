package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.pullUp
import io.github.wtfjb.aximo.domain.StatsTestData.set
import io.github.wtfjb.aximo.domain.StatsTestData.workout
import io.github.wtfjb.aximo.domain.backup.SetsCsv
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.SetType
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SetsCsvTest {

    @Test
    fun onlyTheHeaderWithoutWorkouts() {
        assertEquals(SetsCsv.HEADER.joinToString(",") + "\n", SetsCsv.build(emptyList(), emptyList()))
    }

    @Test
    fun oneRowPerSetOldestWorkoutFirst() {
        val newer = workout(2, at(LocalDate(2026, 10, 2)), pullUp to listOf(set(0.0, 8)), routineId = 7)
        val older = workout(
            1,
            at(LocalDate(2026, 10, 1)),
            bench to listOf(set(60.0, 10, SetType.WARM_UP).copy(position = 0), set(82.5, 8).copy(position = 1, rir = 2)),
        )

        val lines = SetsCsv.build(listOf(newer, older), listOf(Routine(id = 7, name = "Pull A"))).trimEnd().lines()

        assertEquals(4, lines.size)
        assertEquals("1,2026-10-01T10:00:00Z,,Bankdrücken,1,1,WARM_UP,60.0,10,,,1970-01-01T00:00:00Z", lines[1])
        assertEquals("1,2026-10-01T10:00:00Z,,Bankdrücken,1,2,WORKING,82.5,8,2,,1970-01-01T00:00:00Z", lines[2])
        assertEquals("2,2026-10-02T10:00:00Z,Pull A,Klimmzug,1,1,WORKING,0.0,8,,,1970-01-01T00:00:00Z", lines[3])
    }

    @Test
    fun openSetsHaveNoCompletionTime() {
        val detail = workout(1, at(LocalDate(2026, 10, 1)), bench to listOf(set(80.0, 5, done = false)))
        assertEquals("", SetsCsv.build(listOf(detail), emptyList()).trimEnd().lines()[1].substringAfterLast(","))
    }

    @Test
    fun fieldsWithCommasOrQuotesAreQuoted() {
        assertEquals("plain", SetsCsv.escape("plain"))
        assertEquals("\"Push, schwer\"", SetsCsv.escape("Push, schwer"))
        assertEquals("\"Curl \"\"eng\"\"\"", SetsCsv.escape("Curl \"eng\""))
    }
}
