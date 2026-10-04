package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseRepositoryTest : DatabaseTest() {

    private lateinit var repo: RoomExerciseRepository

    @Before
    fun setUp() {
        repo = RoomExerciseRepository(db.exerciseDao())
    }

    @Test
    fun savedExerciseCanBeReadBackWithAllFields() = runTest {
        val id = repo.saveExercise(benchPress())

        assertEquals(benchPress(id = id), repo.getExercise(id))
    }

    @Test
    fun unknownIdReturnsNull() = runTest {
        assertNull(repo.getExercise(42))
    }

    @Test
    fun updateReplacesMuscleGroups() = runTest {
        val id = repo.saveExercise(benchPress())

        val changed = benchPress(id).copy(
            name = "Schrägbankdrücken",
            primaryMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
            secondaryMuscles = emptySet(),
        )
        repo.saveExercise(changed)

        assertEquals(changed, repo.getExercise(id))
    }

    @Test
    fun listIsSortedByNameIgnoringCase() = runTest {
        repo.saveExercise(running())
        repo.saveExercise(benchPress())
        repo.saveExercise(pullUp().copy(name = "klimmzug"))

        val names = repo.observeExercises().first().map { it.name }

        assertEquals(listOf("Bankdrücken", "klimmzug", "Laufen"), names)
    }

    @Test
    fun archivedExercisesAreHiddenByDefault() = runTest {
        val id = repo.saveExercise(benchPress())
        repo.saveExercise(pullUp())

        repo.setArchived(id, true)

        assertEquals(listOf("Klimmzug"), repo.observeExercises().first().map { it.name })
        assertEquals(2, repo.observeExercises(includeArchived = true).first().size)
        assertEquals(true, repo.getExercise(id)?.archived)
    }
}
