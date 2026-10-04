package io.github.wtfjb.aximo.ui.exercises

import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import io.github.wtfjb.aximo.domain.exercise.CatalogSeeder
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSeederTest {

    @Test
    fun seedsOnceOnAnEmptyInstallation() = runTest {
        val repo = FakeExerciseRepository()
        val seeder = CatalogSeeder(repo, FakeSettingsRepository()) { it.name }

        seeder.seedIfEmpty()
        seeder.seedIfEmpty()

        assertEquals(CatalogExercise.entries.size, repo.all.size)
        assertEquals("BENCH_PRESS", repo.all.first { it.catalogId == "strength_bench_press" }.name)
    }

    @Test
    fun ownExercisesPreventTheCatalog() = runTest {
        val own = Exercise(id = 1, name = "Eigene", type = ExerciseType.STRENGTH, equipment = Equipment.MACHINE)
        val repo = FakeExerciseRepository(listOf(own))

        CatalogSeeder(repo, FakeSettingsRepository()) { it.name }.seedIfEmpty()

        assertEquals(listOf(own), repo.all)
    }

    @Test
    fun addMissingOnlyAddsWhatIsNotThere() = runTest {
        val bench = Exercise(id = 1, name = "Bank", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, catalogId = "strength_bench_press", archived = true)
        val repo = FakeExerciseRepository(listOf(bench))
        val seeder = CatalogSeeder(repo, FakeSettingsRepository()) { it.name }

        assertEquals(CatalogExercise.entries.size - 1, seeder.addMissing())
        assertEquals(0, seeder.addMissing())
        assertEquals(1, repo.all.count { it.catalogId == "strength_bench_press" })
    }
}
