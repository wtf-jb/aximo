package io.github.wtfjb.aximo.domain.catalog

import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import io.github.wtfjb.aximo.domain.exercise.normalizeSearch
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.settings.TrainingSettings

/** Searching the library and turning an entry into an own exercise (B-06). */
object CatalogSearch {

    /**
     * Entries whose name contains all words of [query] (case and umlauts ignored)
     * and, with a [region], whose primary muscles lie in it. Names that start with
     * the query come first, the rest is sorted by name.
     */
    fun filter(entries: List<CatalogEntry>, query: String, region: BodyRegion?): List<CatalogEntry> {
        val normalized = normalizeSearch(query)
        val words = normalized.split(' ').filter { it.isNotEmpty() }
        return entries
            .filter { entry ->
                val name = normalizeSearch(entry.name)
                words.all { it in name } && (region == null || entry.primary.any { it.region == region })
            }
            .sortedWith(compareBy({ normalized.isEmpty() || !normalizeSearch(it.name).startsWith(normalized) }, { it.name.lowercase() }))
    }

    /** A new exercise from the entry; increment and rest from the settings, like the starter exercises. */
    fun exerciseFor(entry: CatalogEntry, settings: TrainingSettings): Exercise = Exercise(
        name = entry.name,
        type = entry.type,
        equipment = entry.equipment,
        primaryMuscles = entry.primary,
        secondaryMuscles = entry.secondary,
        incrementKg = settings.steps.forEquipment(entry.equipment),
        repRangeMin = entry.repMin,
        repRangeMax = entry.repMax,
        restSeconds = settings.restSeconds,
        catalogId = entry.catalogId,
    )

    /**
     * The library entry that explains an exercise: its own for exercises added from
     * the library, the closest one for the starter exercises; null for own exercises.
     */
    fun entryFor(entries: List<CatalogEntry>, catalogId: String?): CatalogEntry? {
        val libraryId = when {
            catalogId == null -> return null
            catalogId.startsWith(CatalogEntry.ID_PREFIX) -> catalogId.removePrefix(CatalogEntry.ID_PREFIX)
            else -> CatalogExercise.entries.firstOrNull { it.catalogId == catalogId }?.libraryId() ?: return null
        }
        return entries.firstOrNull { it.id == libraryId }
    }
}

/** The library exercise whose instructions fit a starter exercise best. */
fun CatalogExercise.libraryId(): String = when (this) {
    CatalogExercise.BENCH_PRESS -> "Barbell_Bench_Press_-_Medium_Grip"
    CatalogExercise.INCLINE_BENCH_PRESS -> "Barbell_Incline_Bench_Press_-_Medium_Grip"
    CatalogExercise.DUMBBELL_BENCH_PRESS -> "Dumbbell_Bench_Press"
    CatalogExercise.INCLINE_DUMBBELL_PRESS -> "Incline_Dumbbell_Press"
    CatalogExercise.CABLE_FLY -> "Cable_Crossover"
    CatalogExercise.PUSH_UP -> "Pushups"
    CatalogExercise.DIP -> "Dips_-_Chest_Version"
    CatalogExercise.PULL_UP -> "Pullups"
    CatalogExercise.CHIN_UP -> "Chin-Up"
    CatalogExercise.LAT_PULLDOWN -> "Wide-Grip_Lat_Pulldown"
    CatalogExercise.BARBELL_ROW -> "Bent_Over_Barbell_Row"
    CatalogExercise.DUMBBELL_ROW -> "One-Arm_Dumbbell_Row"
    CatalogExercise.SEATED_CABLE_ROW -> "Seated_Cable_Rows"
    CatalogExercise.DEADLIFT -> "Barbell_Deadlift"
    CatalogExercise.FACE_PULL -> "Face_Pull"
    CatalogExercise.SQUAT -> "Barbell_Squat"
    CatalogExercise.FRONT_SQUAT -> "Front_Squat_Clean_Grip"
    CatalogExercise.LEG_PRESS -> "Leg_Press"
    CatalogExercise.ROMANIAN_DEADLIFT -> "Romanian_Deadlift"
    CatalogExercise.BULGARIAN_SPLIT_SQUAT -> "Split_Squat_with_Dumbbells"
    CatalogExercise.LUNGE -> "Dumbbell_Lunges"
    CatalogExercise.LEG_EXTENSION -> "Leg_Extensions"
    CatalogExercise.LEG_CURL -> "Lying_Leg_Curls"
    CatalogExercise.HIP_THRUST -> "Barbell_Hip_Thrust"
    CatalogExercise.CALF_RAISE -> "Standing_Calf_Raises"
    CatalogExercise.OVERHEAD_PRESS -> "Standing_Military_Press"
    CatalogExercise.DUMBBELL_SHOULDER_PRESS -> "Dumbbell_Shoulder_Press"
    CatalogExercise.LATERAL_RAISE -> "Side_Lateral_Raise"
    CatalogExercise.REVERSE_FLY -> "Bent_Over_Dumbbell_Rear_Delt_Raise_With_Head_On_Bench"
    CatalogExercise.BARBELL_CURL -> "Barbell_Curl"
    CatalogExercise.DUMBBELL_CURL -> "Dumbbell_Alternate_Bicep_Curl"
    CatalogExercise.HAMMER_CURL -> "Hammer_Curls"
    CatalogExercise.TRICEPS_PUSHDOWN -> "Triceps_Pushdown"
    CatalogExercise.SKULL_CRUSHER -> "EZ-Bar_Skullcrusher"
    CatalogExercise.OVERHEAD_TRICEPS_EXTENSION -> "Cable_Rope_Overhead_Triceps_Extension"
    CatalogExercise.HANGING_LEG_RAISE -> "Hanging_Leg_Raise"
    CatalogExercise.CABLE_CRUNCH -> "Cable_Crunch"
    CatalogExercise.AB_WHEEL -> "Ab_Roller"
}
