package io.github.wtfjb.aximo.domain.catalog

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup

/**
 * One exercise of the bundled library (B-06), built from free-exercise-db
 * (`tools/build_exercise_catalog.py`). Names and instructions are English.
 */
data class CatalogEntry(
    /** Id in the library, e.g. "Barbell_Curl". */
    val id: String,
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    val primary: Set<MuscleGroup>,
    val secondary: Set<MuscleGroup>,
    val repMin: Int,
    val repMax: Int,
    val instructions: List<String>,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(type != ExerciseType.CARDIO) { "the library has no cardio exercises" }
        require(primary.isNotEmpty()) { "an exercise needs a primary muscle" }
        require(repMin in 1..repMax) { "rep range must be 1 ≤ min ≤ max" }
    }

    /** The `catalogId` of an exercise created from this entry. */
    val catalogId: String get() = ID_PREFIX + id

    companion object {
        const val ID_PREFIX = "fed_"
    }
}

/** Where the bundled library comes from. Implemented in `:app` (an asset), loaded once. */
fun interface CatalogRepository {
    suspend fun entries(): List<CatalogEntry>
}
