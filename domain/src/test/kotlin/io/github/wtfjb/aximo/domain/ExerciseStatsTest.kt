package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.pullUp
import io.github.wtfjb.aximo.domain.StatsTestData.set
import io.github.wtfjb.aximo.domain.StatsTestData.workout
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.stats.ExerciseStats
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import io.github.wtfjb.aximo.domain.stats.RepRecord
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseStatsTest {

    private val d1 = at(LocalDate(2026, 9, 21))
    private val d2 = at(LocalDate(2026, 9, 24))
    private val d3 = at(LocalDate(2026, 9, 28))

    private val workouts = listOf(
        // newest first, as the repository delivers them
        workout(3, d3, bench to listOf(set(40.0, 10, SetType.WARM_UP), set(82.5, 8), set(82.5, 7))),
        workout(2, d2, bench to listOf(set(80.0, 8), set(80.0, 8)), pullUp to listOf(set(0.0, 10))),
        workout(1, d1, bench to listOf(set(80.0, 7), set(80.0, 8, done = false))),
    )

    @Test
    fun `metric is reps for bodyweight`() {
        assertEquals(ProgressMetric.E1RM, ExerciseStats.metricFor(ExerciseType.STRENGTH))
        assertEquals(ProgressMetric.REPS, ExerciseStats.metricFor(ExerciseType.BODYWEIGHT))
    }

    @Test
    fun `sessions are oldest first with completed sets only`() {
        val sessions = ExerciseStats.sessions(workouts, bench.id)
        assertEquals(listOf(1L, 2L, 3L), sessions.map { it.workoutId })
        assertEquals(1, sessions[0].sets.size)
        assertEquals(3, sessions[2].sets.size) // warm-up stays in the history
    }

    @Test
    fun `running workouts and empty sessions are left out`() {
        val list = listOf(
            workout(5, d3, bench to listOf(set(90.0, 5)), finished = false),
            workout(4, d2, bench to listOf(set(80.0, 8, done = false))),
        )
        assertEquals(emptyList<Long>(), ExerciseStats.sessions(list, bench.id).map { it.workoutId })
    }

    @Test
    fun `entries of the same exercise in one workout are merged`() {
        val list = listOf(workout(1, d1, bench to listOf(set(80.0, 8)), bench to listOf(set(70.0, 10))))
        assertEquals(2, ExerciseStats.sessions(list, bench.id).single().sets.size)
    }

    @Test
    fun `points use the best e1rm per session`() {
        val points = ExerciseStats.points(ExerciseStats.sessions(workouts, bench.id), ProgressMetric.E1RM)
        assertEquals(listOf(1L, 2L, 3L), points.map { it.workoutId })
        assertEquals(80.0 * (1 + 7 / 30.0), points[0].value, 1e-9)
        assertEquals(82.5 * (1 + 8 / 30.0), points[2].value, 1e-9)
    }

    @Test
    fun `sessions with only high-rep sets have no e1rm point`() {
        val list = listOf(workout(1, d1, bench to listOf(set(40.0, 15))))
        assertEquals(0, ExerciseStats.points(ExerciseStats.sessions(list, bench.id), ProgressMetric.E1RM).size)
    }

    @Test
    fun `bodyweight points are the most reps`() {
        val list = listOf(workout(1, d1, pullUp to listOf(set(0.0, 8), set(0.0, 11), set(0.0, 9))))
        assertEquals(11.0, ExerciseStats.points(ExerciseStats.sessions(list, pullUp.id), ProgressMetric.REPS).single().value, 0.0)
    }

    @Test
    fun `change from first to last point`() {
        val points = ExerciseStats.points(ExerciseStats.sessions(workouts, bench.id), ProgressMetric.E1RM)
        val expected = (82.5 * (1 + 8 / 30.0)) / (80.0 * (1 + 7 / 30.0)) - 1
        assertEquals(expected, ExerciseStats.change(points)!!, 1e-9)
        assertNull(ExerciseStats.change(points.take(1)))
    }

    @Test
    fun `bests skip warm-ups`() {
        val bests = ExerciseStats.bests(ExerciseStats.sessions(workouts, bench.id), ProgressMetric.E1RM)
        assertEquals(82.5, bests.maxWeightKg!!, 0.0)
        assertEquals(82.5, bests.bestSet!!.weightKg, 0.0)
        assertEquals(8, bests.bestSet!!.reps)
        assertEquals(80.0 * 7 + 80.0 * 16 + 82.5 * 15, bests.volumeKg, 1e-9)
        assertEquals(7 + 16 + 15, bests.totalReps)
        assertEquals(3, bests.sessions)
    }

    @Test
    fun `best set without e1rm falls back to the heaviest`() {
        val list = listOf(workout(1, d1, bench to listOf(set(40.0, 15), set(45.0, 13))))
        val bests = ExerciseStats.bests(ExerciseStats.sessions(list, bench.id), ProgressMetric.E1RM)
        assertNull(bests.bestE1rm)
        assertEquals(45.0, bests.bestSet!!.weightKg, 0.0)
    }

    @Test
    fun `bodyweight best set has the most reps`() {
        val list = listOf(workout(1, d1, pullUp to listOf(set(10.0, 6), set(0.0, 12), set(5.0, 12))))
        val best = ExerciseStats.bests(ExerciseStats.sessions(list, pullUp.id), ProgressMetric.REPS).bestSet!!
        assertEquals(12, best.reps)
        assertEquals(5.0, best.weightKg, 0.0)
    }

    @Test
    fun `empty sessions give empty bests`() {
        val bests = ExerciseStats.bests(emptyList(), ProgressMetric.E1RM)
        assertNull(bests.bestE1rm)
        assertNull(bests.maxWeightKg)
        assertNull(bests.bestSet)
        assertEquals(0, bests.sessions)
    }

    @Test
    fun `record sessions beat all earlier ones, never the first`() {
        val sessions = ExerciseStats.sessions(workouts, bench.id)
        assertEquals(setOf(2L, 3L), ExerciseStats.recordSessions(sessions, ProgressMetric.E1RM))
        val equal = ExerciseStats.sessions(
            listOf(workout(2, d2, bench to listOf(set(80.0, 8))), workout(1, d1, bench to listOf(set(80.0, 8)))),
            bench.id,
        )
        assertEquals(emptySet<Long>(), ExerciseStats.recordSessions(equal, ProgressMetric.E1RM))
    }

    @Test
    fun `rep records keep only weights not beaten by heavier ones`() {
        val list = listOf(
            workout(
                1,
                d1,
                bench to listOf(
                    set(100.0, 3),
                    set(90.0, 5),
                    set(85.0, 5),
                    set(80.0, 8),
                    set(60.0, 15, SetType.WARM_UP),
                ),
            ),
        )
        assertEquals(
            listOf(RepRecord(100.0, 3), RepRecord(90.0, 5), RepRecord(80.0, 8)),
            ExerciseStats.repRecords(ExerciseStats.sessions(list, bench.id)),
        )
    }

    @Test
    fun `trained exercises are most recent first without cardio`() {
        val run = StatsTestData.bench.copy(id = 9, name = "Laufen", type = ExerciseType.CARDIO, primaryMuscles = emptySet(), secondaryMuscles = emptySet())
        val list = listOf(
            workout(3, d3, bench to listOf(set(80.0, 8))),
            workout(2, d2, pullUp to listOf(set(0.0, 8)), run to listOf(set(0.0, 1))),
            workout(1, d1, pullUp to listOf(set(0.0, 8, done = false))),
        )
        assertEquals(listOf(bench.id, pullUp.id), ExerciseStats.trainedExercises(list).map { it.id })
    }
}
