package io.github.wtfjb.aximo.data.catalog

import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One entry of `exercise_catalog.json` (written by `tools/build_exercise_catalog.py`). */
@Serializable
private data class CatalogRow(
    val id: String,
    val name: String,
    val type: String,
    val equipment: String,
    val primary: List<String>,
    val secondary: List<String> = emptyList(),
    val repMin: Int,
    val repMax: Int,
    val instructions: List<String> = emptyList(),
)

/** One entry of `exercise_catalog_de.json`: the German name and steps of a library entry. */
@Serializable
private data class TranslationRow(
    val id: String,
    val name: String,
    val instructions: List<String> = emptyList(),
)

/** Reads the bundled library. */
object CatalogFile {
    private val json = Json { ignoreUnknownKeys = true }

    /** All valid entries; a row that does not fit the app's model is skipped, not fatal. */
    fun parse(text: String): List<CatalogEntry> =
        json.decodeFromString<List<CatalogRow>>(text).mapNotNull { row ->
            try {
                CatalogEntry(
                    id = row.id,
                    name = row.name,
                    type = ExerciseType.valueOf(row.type),
                    equipment = Equipment.valueOf(row.equipment),
                    primary = row.primary.map { MuscleGroup.valueOf(it) }.toSet(),
                    secondary = row.secondary.map { MuscleGroup.valueOf(it) }.toSet(),
                    repMin = row.repMin,
                    repMax = row.repMax,
                    instructions = row.instructions,
                )
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    /**
     * Shows [entries] in another language: name and steps from [text] where a row
     * with the same id exists, the original name stays searchable as `englishName`.
     * Entries without a translation keep their English texts.
     */
    fun translate(entries: List<CatalogEntry>, text: String): List<CatalogEntry> {
        val rows = json.decodeFromString<List<TranslationRow>>(text).associateBy { it.id }
        return entries.map { entry ->
            val row = rows[entry.id]?.takeIf { it.name.isNotBlank() } ?: return@map entry
            entry.copy(
                name = row.name,
                instructions = row.instructions.ifEmpty { entry.instructions },
            )
        }
    }
}
