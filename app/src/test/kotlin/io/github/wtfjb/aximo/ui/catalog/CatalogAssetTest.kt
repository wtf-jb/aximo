package io.github.wtfjb.aximo.ui.catalog

import io.github.wtfjb.aximo.data.catalog.CatalogFile
import io.github.wtfjb.aximo.domain.catalog.libraryId
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the shipped `exercise_catalog.json`, its German translation and the photos,
 * so a bad regeneration of the files fails the build.
 */
class CatalogAssetTest {

    private val file = File("src/main/assets/exercise_catalog.json")
    private val entries by lazy { CatalogFile.parse(file.readText()) }
    private val germanFile = File("src/main/assets/exercise_catalog_de.json")
    private val imageFolder = File("src/main/assets/exercise_images")

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

    @Test
    fun everyEntryHasAGermanTranslationWithTheSameSteps() {
        val translated = CatalogFile.translate(entries, germanFile.readText())
        val untranslated = entries.zip(translated).filter { (en, de) -> en.name == de.name && en.instructions == de.instructions }
        assertTrue("not translated: ${untranslated.map { it.first.id }}", untranslated.isEmpty())
        val stepCount = entries.zip(translated).filter { (en, de) -> en.instructions.size != de.instructions.size }
        assertTrue("different number of steps: ${stepCount.map { it.first.id }}", stepCount.isEmpty())
        assertTrue(translated.none { entry -> entry.instructions.any { it.isBlank() || '@' in it } })
    }

    @Test
    fun photosBelongToEntriesAndComeInPairs() {
        val ids = entries.map { it.id }.toSet()
        val folders = imageFolder.listFiles().orEmpty().filter { it.isDirectory }
        assertTrue("photo folders without entry", folders.all { it.name in ids })
        assertTrue("incomplete photo pairs", folders.all { folder -> folder.list().orEmpty().sorted() == listOf("0.webp", "1.webp") })
        assertTrue("too few photos: ${folders.size}", folders.size >= entries.size - 10)
    }

    @Test
    fun everyStarterExerciseHasPhotos() {
        val missing = CatalogExercise.entries.filter { !File(imageFolder, "${it.libraryId()}/0.webp").exists() }
        assertTrue("no photos for $missing", missing.isEmpty())
    }
}
