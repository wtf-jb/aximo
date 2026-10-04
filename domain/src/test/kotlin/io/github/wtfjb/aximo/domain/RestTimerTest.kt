package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.rest.NextSet
import io.github.wtfjb.aximo.domain.rest.RestTimer
import io.github.wtfjb.aximo.domain.rest.RestTimerLogic
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {

    private val t0 = Instant.fromEpochSeconds(1_790_000_000)

    private fun exercise(id: Long, name: String, rest: Int) =
        Exercise(id = id, name = name, type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, restSeconds = rest)

    private fun detail(
        weId: Long,
        exercise: Exercise,
        sets: List<SetEntry>,
        superset: String? = null,
    ) = WorkoutExerciseDetail(WorkoutExercise(weId, 1, exercise.id, weId.toInt(), superset), exercise, sets)

    private fun set(id: Long, done: Boolean = false, type: SetType = SetType.WORKING) = SetEntry(
        id = id,
        workoutExerciseId = 1,
        position = id.toInt(),
        weightKg = 80.0,
        reps = 8,
        setType = type,
        completedAt = if (done) t0 else null,
    )

    private val bench = exercise(1, "Bankdrücken", rest = 120)
    private val row = exercise(2, "Rudern", rest = 90)
    private val curl = exercise(3, "Curls", rest = 0)

    // RestTimer

    @Test
    fun remainingRoundsUpAndStopsAtZero() {
        val timer = RestTimer(t0, t0 + 90.seconds, next = null)
        assertEquals(90, timer.remainingSeconds(t0))
        assertEquals(90, timer.remainingSeconds(t0 + 1.milliseconds))
        assertEquals(1, timer.remainingSeconds(t0 + 89.seconds + 999.milliseconds))
        assertEquals(0, timer.remainingSeconds(t0 + 90.seconds))
        assertEquals(0, timer.remainingSeconds(t0 + 100.seconds))
    }

    @Test
    fun fractionRunsFromFullToEmpty() {
        val timer = RestTimer(t0, t0 + 100.seconds, next = null)
        assertEquals(1f, timer.remainingFraction(t0), 0f)
        assertEquals(0.25f, timer.remainingFraction(t0 + 75.seconds), 0.0001f)
        assertEquals(0f, timer.remainingFraction(t0 + 200.seconds), 0f)
        assertFalse(timer.isOver(t0 + 99.seconds))
        assertTrue(timer.isOver(t0 + 100.seconds))
    }

    @Test
    fun extendingMovesTheEnd() {
        val timer = RestTimer(t0, t0 + 60.seconds, next = null).extendedBy(15)
        assertEquals(t0 + 75.seconds, timer.endsAt)
        assertEquals(75, timer.totalSeconds)
    }

    // When a rest starts

    @Test
    fun singleExerciseRestsAfterEverySet() {
        val group = listOf(detail(1, bench, listOf(set(10, done = true), set(11), set(12))))
        assertEquals(120, RestTimerLogic.restAfterCompleting(group, 11))
        assertEquals(120, RestTimerLogic.restAfterCompleting(group, 12))
    }

    @Test
    fun zeroRestMeansNoTimer() {
        val group = listOf(detail(1, curl, listOf(set(10))))
        assertNull(RestTimerLogic.restAfterCompleting(group, 10))
    }

    @Test
    fun unknownSetMeansNoTimer() {
        val group = listOf(detail(1, bench, listOf(set(10))))
        assertNull(RestTimerLogic.restAfterCompleting(group, 99))
    }

    @Test
    fun supersetRestsOnlyAfterTheRoundWithTheLongestRest() {
        val group = listOf(
            detail(1, bench, listOf(set(10), set(11)), superset = "A"),
            detail(2, row, listOf(set(20), set(21)), superset = "A"),
        )
        // First exercise of the round: go straight to the second one.
        assertNull(RestTimerLogic.restAfterCompleting(group, 10))

        val afterFirst = listOf(
            detail(1, bench, listOf(set(10, done = true), set(11)), superset = "A"),
            detail(2, row, listOf(set(20), set(21)), superset = "A"),
        )
        assertEquals(120, RestTimerLogic.restAfterCompleting(afterFirst, 20))
    }

    @Test
    fun supersetRoundWithoutASetForEveryExerciseStillEnds() {
        // Bench has 3 sets, row only 2: round 3 consists of bench alone.
        val group = listOf(
            detail(1, bench, listOf(set(10, true), set(11, true), set(12)), superset = "A"),
            detail(2, row, listOf(set(20, true), set(21, true)), superset = "A"),
        )
        assertEquals(120, RestTimerLogic.restAfterCompleting(group, 12))
    }

    // What comes next

    @Test
    fun nextIsTheFollowingSetOfTheSameExercise() {
        val groups = listOf(listOf(detail(1, bench, listOf(set(10, type = SetType.WARM_UP), set(11), set(12)))))
        assertEquals(NextSet("Bankdrücken", 1), RestTimerLogic.nextSetAfterCompleting(groups, 10))
        // The open warm-up comes first; it is shown as set 0.
        assertEquals(NextSet("Bankdrücken", 0), RestTimerLogic.nextSetAfterCompleting(groups, 11))

        val warmedUp = listOf(listOf(detail(1, bench, listOf(set(10, done = true, type = SetType.WARM_UP), set(11), set(12)))))
        assertEquals(NextSet("Bankdrücken", 2), RestTimerLogic.nextSetAfterCompleting(warmedUp, 11))
    }

    @Test
    fun nextMovesOnToTheFollowingGroupThenWrapsAround() {
        val first = listOf(detail(1, bench, listOf(set(10))))
        val second = listOf(detail(2, row, listOf(set(20, done = true), set(21))))
        val third = listOf(detail(3, curl, listOf(set(30))))
        assertEquals(NextSet("Rudern", 2), RestTimerLogic.nextSetAfterCompleting(listOf(first, second, third), 10))

        val doneFirst = listOf(detail(1, bench, listOf(set(10, done = true))))
        val doneThird = listOf(detail(3, curl, listOf(set(30, done = true))))
        // From the last group back to an earlier one that is still open.
        assertEquals(NextSet("Rudern", 2), RestTimerLogic.nextSetAfterCompleting(listOf(doneFirst, second, third), 30))
        assertEquals(NextSet("Rudern", 2), RestTimerLogic.nextSetAfterCompleting(listOf(doneFirst, second, doneThird), 10))
    }

    @Test
    fun nothingNextWhenEverythingIsDone() {
        val groups = listOf(listOf(detail(1, bench, listOf(set(10, done = true), set(11)))))
        assertNull(RestTimerLogic.nextSetAfterCompleting(groups, 11))
        assertNull(RestTimerLogic.nextSetAfterCompleting(groups, 99))
    }

    @Test
    fun nextInASupersetIsTheNextRound() {
        val groups = listOf(
            listOf(
                detail(1, bench, listOf(set(10, done = true), set(11)), superset = "A"),
                detail(2, row, listOf(set(20), set(21)), superset = "A"),
            ),
        )
        assertEquals(NextSet("Bankdrücken", 2), RestTimerLogic.nextSetAfterCompleting(groups, 20))
    }
}
