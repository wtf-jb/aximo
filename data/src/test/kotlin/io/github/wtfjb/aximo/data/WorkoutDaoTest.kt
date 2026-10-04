package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.db.entity.SetEntryEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutExerciseEntity
import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.domain.model.SetType
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutDaoTest : DatabaseTest() {

    private val start = Instant.fromEpochMilliseconds(1_790_000_000_000)

    @Test
    fun setsAreStoredWithTypesAndTimestamps() = runTest {
        val benchId = RoomExerciseRepository(db.exerciseDao()).saveExercise(benchPress())
        val dao = db.workoutDao()
        val workoutId = dao.insert(WorkoutEntity(startedAt = start, endedAt = null, routineId = null, note = "", rating = null))
        val weId = dao.insertExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = benchId, position = 0, supersetGroup = null, note = ""),
        )
        dao.insertSet(SetEntryEntity(workoutExerciseId = weId, position = 1, weightKg = 82.5, reps = 8, rpe = null, rir = 2, setType = SetType.WORKING, completedAt = start))
        dao.insertSet(SetEntryEntity(workoutExerciseId = weId, position = 0, weightKg = 40.0, reps = 10, rpe = null, rir = null, setType = SetType.WARM_UP, completedAt = null))

        val sets = dao.getSets(weId)

        assertEquals(listOf(SetType.WARM_UP, SetType.WORKING), sets.map { it.setType })
        assertEquals(82.5, sets[1].weightKg, 0.0)
        assertEquals(start, sets[1].completedAt)
        assertNull(sets[0].completedAt)
    }

    @Test
    fun deletingAWorkoutDeletesItsExercisesAndSets() = runTest {
        val benchId = RoomExerciseRepository(db.exerciseDao()).saveExercise(benchPress())
        val dao = db.workoutDao()
        val workoutId = dao.insert(WorkoutEntity(startedAt = start, endedAt = null, routineId = null, note = "", rating = null))
        val weId = dao.insertExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = benchId, position = 0, supersetGroup = null, note = ""),
        )
        dao.insertSet(SetEntryEntity(workoutExerciseId = weId, position = 0, weightKg = 80.0, reps = 8, rpe = null, rir = null, setType = SetType.WORKING, completedAt = start))

        dao.delete(workoutId)

        assertNull(dao.getById(workoutId))
        assertEquals(emptyList<Any>(), dao.getExercises(workoutId))
        assertEquals(emptyList<Any>(), dao.getSets(weId))
    }
}
