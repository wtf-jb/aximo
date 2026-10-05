package io.github.wtfjb.aximo.domain.catalog

import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSearchTest {

    private fun entry(id: String, name: String, equipment: Equipment = Equipment.BARBELL, vararg primary: MuscleGroup) = CatalogEntry(
        id = id,
        name = name,
        type = if (equipment == Equipment.BODYWEIGHT) ExerciseType.BODYWEIGHT else ExerciseType.STRENGTH,
        equipment = equipment,
        primary = primary.toSet().ifEmpty { setOf(MuscleGroup.CHEST) },
        secondary = setOf(MuscleGroup.TRICEPS),
        repMin = 6,
        repMax = 10,
        instructions = listOf("Step one.", "Step two."),
    )

    private val entries = listOf(
        entry("Barbell_Curl", "Barbell Curl", primary = arrayOf(MuscleGroup.BICEPS)),
        entry("Bench", "Barbell Bench Press", primary = arrayOf(MuscleGroup.CHEST)),
        entry("Bench_Dips", "Bench Dips", Equipment.BODYWEIGHT, MuscleGroup.TRICEPS),
        entry("Bent_Over_Row", "Bent Over Barbell Row", primary = arrayOf(MuscleGroup.LATS)),
        entry("Bar_Hang", "Bar Hang", primary = arrayOf(MuscleGroup.LATS)),
    )

    @Test
    fun emptyQueryReturnsAllSortedByName() {
        assertEquals(
            listOf("Bar Hang", "Barbell Bench Press", "Barbell Curl", "Bench Dips", "Bent Over Barbell Row"),
            CatalogSearch.filter(entries, "", null).map { it.name },
        )
    }

    @Test
    fun allWordsMustMatchAndCaseIsIgnored() {
        assertEquals(listOf("Barbell Bench Press"), CatalogSearch.filter(entries, "  BENCH  barbell", null).map { it.name })
        assertTrue(CatalogSearch.filter(entries, "squat", null).isEmpty())
    }

    @Test
    fun translatedEntriesAreFoundByGermanAndEnglishName() {
        val translated = listOf(entry("Bench", "Bankdrücken").copy(englishName = "Barbell Bench Press"))
        assertEquals(listOf("Bankdrücken"), CatalogSearch.filter(translated, "bankdrucken", null).map { it.name })
        assertEquals(listOf("Bankdrücken"), CatalogSearch.filter(translated, "bench press", null).map { it.name })
        // words from both names together don't count as a match
        assertTrue(CatalogSearch.filter(translated, "bankdrücken press", null).isEmpty())
    }

    @Test
    fun namesStartingWithTheQueryComeFirst() {
        assertEquals(
            listOf("Bench Dips", "Barbell Bench Press"),
            CatalogSearch.filter(entries, "bench", null).map { it.name },
        )
    }

    @Test
    fun regionLooksAtPrimaryMuscles() {
        assertEquals(listOf("Bar Hang", "Bent Over Barbell Row"), CatalogSearch.filter(entries, "", BodyRegion.BACK).map { it.name })
        assertEquals(listOf("Barbell Curl", "Bench Dips"), CatalogSearch.filter(entries, "", BodyRegion.ARMS).map { it.name })
    }

    @Test
    fun exerciseKeepsLibraryDataAndTakesStepsFromSettings() {
        val exercise = CatalogSearch.exerciseFor(entries[1], TrainingSettings(restSeconds = 150))

        assertEquals("Barbell Bench Press", exercise.name)
        assertEquals(Equipment.BARBELL, exercise.equipment)
        assertEquals(setOf(MuscleGroup.CHEST), exercise.primaryMuscles)
        assertEquals(setOf(MuscleGroup.TRICEPS), exercise.secondaryMuscles)
        assertEquals(6, exercise.repRangeMin)
        assertEquals(10, exercise.repRangeMax)
        assertEquals(150, exercise.restSeconds)
        assertEquals("fed_Bench", exercise.catalogId)
        assertEquals(TrainingSettings().steps.forEquipment(Equipment.BARBELL), exercise.incrementKg, 0.0)
    }

    @Test
    fun instructionsForLibraryAndStarterExercises() {
        val library = listOf(entry("Barbell_Curl", "Barbell Curl"), entry("Bent_Over_Barbell_Row", "Row"))

        assertEquals("Barbell Curl", CatalogSearch.entryFor(library, "fed_Barbell_Curl")?.name)
        assertEquals("Barbell Curl", CatalogSearch.entryFor(library, CatalogExercise.BARBELL_CURL.catalogId)?.name)
        assertEquals("Row", CatalogSearch.entryFor(library, CatalogExercise.BARBELL_ROW.catalogId)?.name)
        assertNull(CatalogSearch.entryFor(library, null))
        assertNull(CatalogSearch.entryFor(library, "cardio_running"))
        assertNull(CatalogSearch.entryFor(library, "fed_Unknown"))
    }

    @Test
    fun entryNeedsPrimaryMuscleAndValidReps() {
        val valid = entries[0]
        runCatching { valid.copy(primary = emptySet()) }.let { assertTrue(it.isFailure) }
        runCatching { valid.copy(repMin = 11) }.let { assertTrue(it.isFailure) }
        runCatching { valid.copy(type = ExerciseType.CARDIO) }.let { assertTrue(it.isFailure) }
    }
}
