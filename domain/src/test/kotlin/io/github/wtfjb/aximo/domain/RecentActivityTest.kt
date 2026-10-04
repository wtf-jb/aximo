package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.recent.RecentActivity
import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentActivityTest {

    private val t0 = Instant.fromEpochSeconds(1_790_000_000)
    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val run = Exercise(id = 2, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)
    private val pull = Routine(id = 5, name = "Pull A", position = 0)

    private fun workout(id: Long, start: Instant, minutes: Int?, routineId: Long? = null): WorkoutDetail {
        val sets = listOf(
            SetEntry(id = 1, workoutExerciseId = 1, position = 0, weightKg = 40.0, reps = 10, setType = SetType.WARM_UP, completedAt = start),
            SetEntry(id = 2, workoutExerciseId = 1, position = 1, weightKg = 80.0, reps = 8, completedAt = start),
            SetEntry(id = 3, workoutExerciseId = 1, position = 2, weightKg = 80.0, reps = 8, completedAt = null),
        )
        return WorkoutDetail(
            workout = Workout(id = id, startedAt = start, endedAt = minutes?.let { start + it.minutes }, routineId = routineId),
            exercises = listOf(WorkoutExerciseDetail(WorkoutExercise(1, id, bench.id, 0), bench, sets)),
        )
    }

    private fun cardio(id: Long, start: Instant, exerciseId: Long = run.id) =
        CardioEntry(id = id, exerciseId = exerciseId, startedAt = start, durationSec = 1721, distanceM = 5200.0)

    @Test
    fun `merges workouts and cardio newest first`() {
        val items = RecentActivity.merge(
            workouts = listOf(workout(1, t0, 52, routineId = pull.id)),
            routines = listOf(pull),
            cardio = listOf(cardio(9, t0 - 2.days)),
            exercises = listOf(bench, run),
        )
        val first = items[0] as RecentItem.WorkoutItem
        assertEquals("Pull A", first.routineName)
        assertEquals(52 * 60L, first.durationSec)
        assertEquals(640.0, first.volumeKg, 0.001) // only the completed working set counts
        assertEquals(9L, (items[1] as RecentItem.CardioItem).entry.id)
    }

    @Test
    fun `running workouts and unknown activities are skipped`() {
        val items = RecentActivity.merge(
            workouts = listOf(workout(1, t0, null)),
            routines = emptyList(),
            cardio = listOf(cardio(9, t0, exerciseId = 99)),
            exercises = listOf(bench, run),
        )
        assertEquals(emptyList<RecentItem>(), items)
    }

    @Test
    fun `free workout has no routine name and the list is limited`() {
        val workouts = (1..4).map { workout(it.toLong(), t0 + it.days, 30) }
        val runs = (1..4).map { cardio(it.toLong(), t0 - it.days) }
        val items = RecentActivity.merge(workouts, emptyList(), runs, listOf(bench, run))
        assertEquals(RecentActivity.DEFAULT_LIMIT, items.size)
        assertNull((items[0] as RecentItem.WorkoutItem).routineName)
        assertEquals(t0 + 4.days, items[0].startedAt)
    }
}
