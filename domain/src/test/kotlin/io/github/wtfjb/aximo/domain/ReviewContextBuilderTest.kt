package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.StatsTestData.at
import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.StatsTestData.pullUp
import io.github.wtfjb.aximo.domain.StatsTestData.set
import io.github.wtfjb.aximo.domain.StatsTestData.workout
import io.github.wtfjb.aximo.domain.StatsTestData.zone
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.review.EffortStatus
import io.github.wtfjb.aximo.domain.review.ReviewContextBuilder
import io.github.wtfjb.aximo.domain.review.TrendStatus
import io.github.wtfjb.aximo.domain.review.VolumeStatus
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewContextBuilderTest {

    private val today = LocalDate(2026, 10, 4)
    private val push = RoutineWithExercises(
        Routine(id = 7, name = "Push A"),
        listOf(RoutineExercise(routineId = 7, exerciseId = bench.id, position = 0, targetSets = 3, repMin = 6, repMax = 8, targetRir = 2)),
    )

    private fun benchDay(id: Long, date: LocalDate, weight: Double, rir: Int? = null) = workout(
        id, at(date),
        bench to listOf(set(40.0, 10, SetType.WARM_UP), set(weight, 8).copy(rir = rir), set(weight, 8).copy(rir = rir)),
        routineId = 7,
    )

    @Test
    fun emptyHistory() {
        val context = ReviewContextBuilder.build(emptyList(), listOf(push), listOf(bench), weeklyGoal = 3, today = today, zone = zone)

        assertTrue(context.isEmpty)
        assertEquals(6, context.weeks)
        assertEquals(3, context.weeklyGoal)
        assertEquals("Bankdrücken", context.routines.single().exercises.single().name)
        assertTrue(context.exercises.isEmpty())
        assertNull(context.effort.avgRirFirstHalf)
    }

    @Test
    fun aggregatesOnlyThePeriod() {
        val workouts = listOf(
            benchDay(1, LocalDate(2026, 8, 1), 100.0), // older than 6 weeks
            benchDay(2, LocalDate(2026, 8, 24), 80.0, rir = 3), // first day of the period
            benchDay(3, LocalDate(2026, 9, 7), 82.5, rir = 3),
            benchDay(4, LocalDate(2026, 9, 21), 82.5, rir = 1),
            benchDay(5, LocalDate(2026, 10, 1), 82.5, rir = 1),
            workout(6, at(LocalDate(2026, 10, 2)), pullUp to listOf(set(0.0, 10), set(0.0, 8)), finished = false), // running
        )

        val context = ReviewContextBuilder.build(workouts, listOf(push), listOf(bench, pullUp), null, today, zone)

        assertEquals(4, context.sessions)
        assertEquals(0.7, context.sessionsPerWeek, 0.0)
        assertEquals(4, context.routines.single().sessions)

        val trend = context.exercises.single()
        assertEquals(ProgressMetric.E1RM, trend.metric)
        assertEquals(4, trend.sessions)
        assertEquals(101.3, trend.first, 0.0) // 80 × (1 + 8/30)
        assertEquals(104.5, trend.last, 0.0)
        assertEquals(2, trend.sessionsSinceBest)
        assertEquals(82.5, trend.lastTopWeightKg, 0.0)
        assertEquals(8, trend.lastTopReps)
        assertEquals(2.0, trend.avgRir!!, 0.0)

        // Bench: 2 working sets per session → chest counts 1, arms and shoulders 0.5; 8 sets / 6 weeks.
        val chest = context.volume.first { it.region == BodyRegion.CHEST }
        assertEquals(1.3, chest.setsPerWeek, 0.0)
        assertEquals(3.0, context.effort.avgRirFirstHalf!!, 0.0)
        assertEquals(1.0, context.effort.avgRirSecondHalf!!, 0.0)

        assertEquals(listOf(bench.id, pullUp.id), context.available.map { it.exerciseId })
    }

    @Test
    fun sessionsSinceBest() {
        assertEquals(0, ReviewContextBuilder.sessionsSinceBest(listOf(1.0)))
        assertEquals(0, ReviewContextBuilder.sessionsSinceBest(listOf(1.0, 2.0, 3.0)))
        assertEquals(2, ReviewContextBuilder.sessionsSinceBest(listOf(4.0, 5.0, 5.0, 5.0)))
        assertEquals(3, ReviewContextBuilder.sessionsSinceBest(listOf(6.0, 5.0, 5.5, 4.0)))
    }

    @Test
    fun archivedAndCardioAreNotOffered() {
        val archived = pullUp.copy(id = 9, archived = true)
        val run = bench.copy(id = 10, name = "Laufen", type = io.github.wtfjb.aximo.domain.model.ExerciseType.CARDIO)

        val context = ReviewContextBuilder.build(emptyList(), emptyList(), listOf(bench, archived, run), null, today, zone)

        assertEquals(listOf(bench.id), context.available.map { it.exerciseId })
    }

    @Test
    fun signalsAndBreaks() {
        val workouts = listOf(
            benchDay(2, LocalDate(2026, 8, 24), 80.0, rir = 3),
            benchDay(3, LocalDate(2026, 9, 7), 82.5, rir = 3),
            benchDay(4, LocalDate(2026, 9, 21), 85.0, rir = 2),
            benchDay(5, LocalDate(2026, 10, 1), 87.5, rir = 2),
        )

        val context = ReviewContextBuilder.build(workouts, listOf(push), listOf(bench), null, today, zone)

        assertEquals(3, context.daysSinceLastSession)
        assertEquals(14, context.longestBreakDays)
        val trend = context.exercises.single()
        assertEquals(3, trend.daysSinceLast)
        assertEquals(TrendStatus.PROGRESSING, trend.status)
        assertTrue(trend.newBest)
        assertEquals(4, trend.recent.size)
        assertEquals(trend.last, trend.recent.last(), 0.0)
        assertEquals(0.0, trend.dropFromBestPct, 0.0)
        assertEquals(9.4, trend.changePct!!, 0.1)
        // Target of routine 7: 6–8 reps, RIR 2. All sets reach 8 reps in all four sessions; the last sessions had RIR 3, 2, 2.
        assertEquals(8, trend.repMax)
        assertEquals(2, trend.targetRir)
        assertEquals(4, trend.sessionsAtRepCeiling)
        assertEquals(2.3, trend.recentAvgRir!!, 0.0)
        assertEquals(0.3, trend.rirVsTarget!!, 0.0)
        assertEquals(EffortStatus.ON_TARGET, trend.effort)
        assertEquals(VolumeStatus.BELOW, context.volume.first { it.region == BodyRegion.CHEST }.status)
    }

    @Test
    fun freeWorkoutHasNoTarget() {
        val free = workout(
            9, at(LocalDate(2026, 10, 1)),
            bench to listOf(set(90.0, 8).copy(rir = 2), set(90.0, 8).copy(rir = 2)),
        )
        val workouts = listOf(
            benchDay(2, LocalDate(2026, 9, 7), 82.5, rir = 3),
            benchDay(3, LocalDate(2026, 9, 21), 85.0, rir = 2),
            free,
        )

        val trend = ReviewContextBuilder.build(workouts, listOf(push), listOf(bench), null, today, zone).exercises.single()

        assertNull(trend.repMax)
        assertNull(trend.targetRir)
        assertEquals(0, trend.sessionsAtRepCeiling)
        assertNull(trend.rirVsTarget)
        assertNull(trend.effort)
        assertNotNull(trend.recentAvgRir)
    }

    @Test
    fun targetComesFromTheRoutineOfTheLastSession() {
        val pull = RoutineWithExercises(
            Routine(id = 8, name = "Upper"),
            listOf(RoutineExercise(routineId = 8, exerciseId = bench.id, position = 0, targetSets = 3, repMin = 5, repMax = 5, targetRir = 1)),
        )
        val workouts = listOf(
            benchDay(2, LocalDate(2026, 9, 7), 82.5),
            benchDay(3, LocalDate(2026, 9, 21), 85.0),
            benchDay(4, LocalDate(2026, 10, 1), 87.5).let { it.copy(workout = it.workout.copy(routineId = 8)) },
        )

        val trend = ReviewContextBuilder.build(workouts, listOf(push, pull), listOf(bench), null, today, zone).exercises.single()

        assertEquals(5, trend.repMax)
        assertEquals(1, trend.targetRir)
    }

    @Test
    fun exerciseNoLongerInTheRoutineHasNoTarget() {
        val empty = RoutineWithExercises(Routine(id = 7, name = "Push A"), emptyList())
        val workouts = listOf(
            benchDay(2, LocalDate(2026, 9, 7), 82.5),
            benchDay(3, LocalDate(2026, 9, 21), 85.0),
        )

        val trend = ReviewContextBuilder.build(workouts, listOf(empty), listOf(bench), null, today, zone).exercises.single()

        assertNull(trend.repMax)
    }

    @Test
    fun noBreakWithoutSessions() {
        val context = ReviewContextBuilder.build(emptyList(), listOf(push), listOf(bench), null, today, zone)

        assertNull(context.daysSinceLastSession)
        assertEquals(0, context.longestBreakDays)
    }
}
