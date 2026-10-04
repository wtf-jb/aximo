package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import io.github.wtfjb.aximo.domain.exercise.ExerciseCatalog
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {

    private val exercises = ExerciseCatalog.exercises(TrainingSettings(restSeconds = 90)) { it.name }

    @Test
    fun everyEntryIsAValidExercise() {
        // Constructing Exercise checks the rules (rep range, muscles not primary and secondary).
        assertEquals(CatalogExercise.entries.size, exercises.size)
        assertTrue(exercises.all { it.primaryMuscles.isNotEmpty() })
        assertTrue(exercises.none { it.type == ExerciseType.CARDIO })
    }

    @Test
    fun catalogIdsAreUniqueAndDontClashWithCardio() {
        val ids = exercises.map { it.catalogId!! }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("strength_") })
        assertEquals("strength_bench_press", CatalogExercise.BENCH_PRESS.catalogId)
    }

    @Test
    fun incrementAndRestComeFromTheSettings() {
        val bench = exercises.first { it.catalogId == CatalogExercise.BENCH_PRESS.catalogId }
        val curl = exercises.first { it.catalogId == CatalogExercise.DUMBBELL_CURL.catalogId }
        assertEquals(2.5, bench.incrementKg, 0.0)
        assertEquals(2.0, curl.incrementKg, 0.0)
        assertEquals(90, bench.restSeconds)
    }

    @Test
    fun seedOnlyWithoutStrengthOrBodyweightExercises() {
        val run = Exercise(name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)
        val own = Exercise(name = "Eigene", type = ExerciseType.STRENGTH, equipment = Equipment.MACHINE, archived = true)
        assertTrue(ExerciseCatalog.needsSeed(emptyList()))
        assertTrue(ExerciseCatalog.needsSeed(listOf(run)))
        assertFalse(ExerciseCatalog.needsSeed(listOf(run, own)))
    }
}
