package io.github.wtfjb.aximo.ui.catalog

import io.github.wtfjb.aximo.data.catalog.CatalogFile
import io.github.wtfjb.aximo.domain.catalog.libraryId
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the shipped `exercise_catalog.json`, so a bad regeneration of the file fails the build. */
class CatalogAssetTest {

    private val file = File("src/main/assets/exercise_catalog.json")
    private val entries by lazy { CatalogFile.parse(file.readText()) }

    @Test
    fun everyRowIsValidAndIdsAreUnique() {
        val rows = file.readText().split("\"id\":").size - 1
        assertEquals("rows that do not fit the model were skipped", rows, entries.size)
        assertEquals(entries.size, entries.map { it.id }.toSet().size)
        assertTrue("library looks too small: ${entries.size}", entries.size >= 600)
    }

    @Test
    fun everyEntryHasInstructions() {
        assertTrue(entries.filter { it.instructions.isEmpty() }.map { it.id }.isEmpty())
    }

    @Test
    fun everyStarterExerciseHasALibraryEntry() {
        val ids = entries.map { it.id }.toSet()
        val missing = CatalogExercise.entries.filter { it.libraryId() !in ids }
        assertTrue("no library entry for $missing", missing.isEmpty())
    }
}
