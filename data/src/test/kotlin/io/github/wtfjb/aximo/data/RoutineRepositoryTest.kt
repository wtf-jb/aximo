package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.data.repository.RoomRoutineRepository
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutineRepositoryTest : DatabaseTest() {

    private lateinit var routines: RoomRoutineRepository
    private lateinit var exercises: RoomExerciseRepository
    private var benchId = 0L
    private var pullUpId = 0L

    @Before
    fun setUp() = runTest {
        routines = RoomRoutineRepository(db.routineDao())
        exercises = RoomExerciseRepository(db.exerciseDao())
        benchId = exercises.saveExercise(benchPress())
        pullUpId = exercises.saveExercise(pullUp())
    }

    private fun target(exerciseId: Long, position: Int, superset: String? = null) = RoutineExercise(
        exerciseId = exerciseId,
        position = position,
        targetSets = 3,
        repMin = 6,
        repMax = 8,
        targetRir = 2,
        supersetGroup = superset,
    )

    @Test
    fun routineIsSavedWithExercisesInOrder() = runTest {
        val id = routines.saveRoutine(
            Routine(name = "Push A"),
            listOf(target(pullUpId, 1, "A"), target(benchId, 0, "A")),
        )

        val loaded = routines.getRoutine(id)!!

        assertEquals("Push A", loaded.routine.name)
        assertEquals(listOf(benchId, pullUpId), loaded.exercises.map { it.exerciseId })
        assertEquals(listOf("A", "A"), loaded.exercises.map { it.supersetGroup })
    }

    @Test
    fun savingAgainReplacesTheExercises() = runTest {
        val id = routines.saveRoutine(Routine(name = "Push A"), listOf(target(benchId, 0), target(pullUpId, 1)))

        routines.saveRoutine(Routine(id = id, name = "Push B"), listOf(target(pullUpId, 0)))

        val loaded = routines.getRoutine(id)!!
        assertEquals("Push B", loaded.routine.name)
        assertEquals(listOf(pullUpId), loaded.exercises.map { it.exerciseId })
    }

    @Test
    fun routinesAreOrderedByPosition() = runTest {
        routines.saveRoutine(Routine(name = "Legs", position = 2), emptyList())
        routines.saveRoutine(Routine(name = "Push", position = 0), emptyList())
        routines.saveRoutine(Routine(name = "Pull", position = 1), emptyList())

        assertEquals(listOf("Push", "Pull", "Legs"), routines.observeRoutines().first().map { it.name })
    }

    @Test
    fun deletingARoutineDeletesItsExercises() = runTest {
        val id = routines.saveRoutine(Routine(name = "Push A"), listOf(target(benchId, 0)))

        routines.deleteRoutine(id)

        assertNull(routines.getRoutine(id))
        assertEquals(emptyList<Any>(), db.routineDao().getExercises(id))
    }
}
