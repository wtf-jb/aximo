package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.data.repository.RoomRoutineRepository
import io.github.wtfjb.aximo.data.repository.RoomWorkoutRepository
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutRepositoryTest : DatabaseTest() {

    private lateinit var workouts: RoomWorkoutRepository
    private var benchId = 0L
    private var pullUpId = 0L
    private val t0 = Instant.fromEpochSeconds(1_790_000_000)

    @Before
    fun setUp() = runTest {
        workouts = RoomWorkoutRepository(db.workoutDao())
        val exercises = RoomExerciseRepository(db.exerciseDao())
        benchId = exercises.saveExercise(benchPress())
        pullUpId = exercises.saveExercise(pullUp())
    }

    @Test
    fun activeWorkoutContainsExercisesAndSetsInOrder() = runTest {
        val id = workouts.startWorkout(t0)
        workouts.addExercise(id, benchId, null, listOf(PlannedSet(40.0, 10, setType = SetType.WARM_UP), PlannedSet(80.0, 8)))
        workouts.addExercise(id, pullUpId, "A", listOf(PlannedSet(0.0, 8)))

        val active = workouts.observeActiveWorkout().first()!!

        assertEquals(id, active.workout.id)
        assertEquals(listOf("Bankdrücken", "Klimmzug"), active.exercises.map { it.exercise.name })
        assertEquals(listOf(SetType.WARM_UP, SetType.WORKING), active.exercises[0].sets.map { it.setType })
        assertEquals("A", active.exercises[1].entry.supersetGroup)
    }

    @Test
    fun finishedWorkoutIsNoLongerActive() = runTest {
        val id = workouts.startWorkout(t0)

        workouts.finishWorkout(id, t0.plus(kotlin.time.Duration.parse("1h")), " gut ")

        assertNull(workouts.observeActiveWorkout().first())
        assertEquals("gut", db.workoutDao().getById(id)!!.note)
    }

    @Test
    fun recentFinishedSkipsRunningWorkouts() = runTest {
        val older = workouts.startWorkout(t0)
        workouts.addExercise(older, benchId, null, listOf(PlannedSet(80.0, 8)))
        workouts.finishWorkout(older, t0.plus(kotlin.time.Duration.parse("1h")), "")
        val newer = workouts.startWorkout(t0.plus(kotlin.time.Duration.parse("1d")))
        workouts.finishWorkout(newer, t0.plus(kotlin.time.Duration.parse("25h")), "")
        workouts.startWorkout(t0.plus(kotlin.time.Duration.parse("2d")))

        val recent = workouts.observeRecentFinished(5).first()

        assertEquals(listOf(newer, older), recent.map { it.workout.id })
        assertEquals("Bankdrücken", recent[1].exercises[0].exercise.name)
    }

    @Test
    fun finishedListsAllFinishedWorkoutsNewestFirst() = runTest {
        val older = workouts.startWorkout(t0)
        workouts.addExercise(older, benchId, null, listOf(PlannedSet(80.0, 8)))
        workouts.finishWorkout(older, t0.plus(kotlin.time.Duration.parse("1h")), "")
        val newer = workouts.startWorkout(t0.plus(kotlin.time.Duration.parse("1d")))
        workouts.finishWorkout(newer, t0.plus(kotlin.time.Duration.parse("25h")), "")
        workouts.startWorkout(t0.plus(kotlin.time.Duration.parse("2d")))

        val finished = workouts.observeFinished().first()

        assertEquals(listOf(newer, older), finished.map { it.workout.id })
        assertEquals(1, finished[1].exercises[0].sets.size)
    }

    @Test
    fun previousOfRoutineIsTheLastFinishedBefore() = runTest {
        val routines = RoomRoutineRepository(db.routineDao())
        val push = routines.saveRoutine(Routine(name = "Push A"), emptyList())
        val pull = routines.saveRoutine(Routine(name = "Pull A"), emptyList())
        val first = workouts.startWorkout(t0, push)
        workouts.finishWorkout(first, t0.plus(kotlin.time.Duration.parse("1h")), "")
        val second = workouts.startWorkout(t0.plus(kotlin.time.Duration.parse("1d")), push)
        workouts.finishWorkout(second, t0.plus(kotlin.time.Duration.parse("25h")), "")
        val other = workouts.startWorkout(t0.plus(kotlin.time.Duration.parse("2d")), pull)
        workouts.finishWorkout(other, t0.plus(kotlin.time.Duration.parse("49h")), "")
        val current = t0.plus(kotlin.time.Duration.parse("3d"))

        assertEquals(second, workouts.previousOfRoutine(push, current)?.workout?.id)
        assertEquals(first, workouts.previousOfRoutine(push, t0.plus(kotlin.time.Duration.parse("1d")))?.workout?.id)
        assertNull(workouts.previousOfRoutine(push, t0))
    }

    @Test
    fun setsCanBeCompletedEditedAndDeleted() = runTest {
        val id = workouts.startWorkout(t0)
        val weId = workouts.addExercise(id, benchId, null, listOf(PlannedSet(80.0, 8)))
        val added = workouts.addSet(weId, PlannedSet(80.0, 7))

        val first = workouts.observeActiveWorkout().first()!!.exercises[0].sets[0]
        workouts.updateSet(first.copy(reps = 9, completedAt = t0))
        workouts.deleteSet(added)

        val sets = workouts.observeActiveWorkout().first()!!.exercises[0].sets
        assertEquals(1, sets.size)
        assertEquals(9, sets[0].reps)
        assertEquals(t0, sets[0].completedAt)
    }

    @Test
    fun lastSessionComesFromTheNewestFinishedWorkout() = runTest {
        val old = workouts.startWorkout(t0)
        workouts.addExercise(old, benchId, null, listOf(PlannedSet(75.0, 8)))
        workouts.finishWorkout(old, t0, "")

        val newer = workouts.startWorkout(t0.plus(kotlin.time.Duration.parse("2d")))
        workouts.addExercise(newer, benchId, null, listOf(PlannedSet(80.0, 8), PlannedSet(80.0, 7)))
        workouts.finishWorkout(newer, t0.plus(kotlin.time.Duration.parse("2d")), "")

        val current = workouts.startWorkout(t0.plus(kotlin.time.Duration.parse("4d")))
        workouts.addExercise(current, benchId, null, listOf(PlannedSet(85.0, 5)))

        val last = workouts.lastSessionSets(benchId, excludeWorkoutId = current)

        assertEquals(listOf(80.0, 80.0), last.map { it.weightKg })
        assertEquals(emptyList<Any>(), workouts.lastSessionSets(pullUpId, current))
    }

    @Test
    fun removingAnExerciseDeletesItsSets() = runTest {
        val id = workouts.startWorkout(t0)
        val weId = workouts.addExercise(id, benchId, null, listOf(PlannedSet(80.0, 8)))

        workouts.removeExercise(weId)

        assertEquals(emptyList<Any>(), workouts.observeActiveWorkout().first()!!.exercises)
        assertEquals(emptyList<Any>(), db.workoutDao().getSets(weId))
    }

    @Test
    fun lastWorkoutPerRoutineOnlyCountsFinishedWorkouts() = runTest {
        val routineId = io.github.wtfjb.aximo.data.repository.RoomRoutineRepository(db.routineDao())
            .saveRoutine(io.github.wtfjb.aximo.domain.model.Routine(name = "Push"), emptyList())
        val first = workouts.startWorkout(t0, routineId)
        workouts.finishWorkout(first, t0, "")
        val later = t0.plus(kotlin.time.Duration.parse("1d"))
        val second = workouts.startWorkout(later, routineId)
        workouts.finishWorkout(second, later, "")
        workouts.startWorkout(later.plus(kotlin.time.Duration.parse("1d")), routineId) // still running

        assertEquals(mapOf(routineId to later), workouts.observeLastWorkoutPerRoutine().first())
    }

    @Test
    fun workoutByIdRatingAndHistoryBefore() = runTest {
        val old = workouts.startWorkout(t0)
        val weOld = workouts.addExercise(old, benchId, null, listOf(PlannedSet(75.0, 8)))
        workouts.finishWorkout(old, t0, "")
        val now = t0.plus(kotlin.time.Duration.parse("2d"))
        val current = workouts.startWorkout(now)
        workouts.addExercise(current, benchId, null, listOf(PlannedSet(80.0, 8)))

        workouts.rateWorkout(current, 4, " gut ")

        val detail = workouts.getWorkout(current)!!
        assertEquals(4, detail.workout.rating)
        assertEquals("gut", detail.workout.note)
        assertEquals(listOf(weOld), workouts.setsBefore(benchId, now).map { it.workoutExerciseId })
        assertEquals(emptyList<Any>(), workouts.setsBefore(benchId, t0))
        assertNull(workouts.getWorkout(9999))
    }

    @Test
    fun progressionIsStoredPerExercise() = runTest {
        val repo = io.github.wtfjb.aximo.data.repository.RoomProgressionRepository(db.progressionDao())
        val state = io.github.wtfjb.aximo.domain.model.ProgressionState(
            benchId, 82.5, 6, io.github.wtfjb.aximo.domain.model.ProgressionReason.INCREASE_WEIGHT,
        )
        repo.save(state)
        repo.save(state.copy(nextWeightKg = 85.0))

        assertEquals(85.0, repo.get(benchId)!!.nextWeightKg, 0.0)
        assertNull(repo.get(pullUpId))
    }
}
