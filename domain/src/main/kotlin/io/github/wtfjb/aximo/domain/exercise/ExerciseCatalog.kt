package io.github.wtfjb.aximo.domain.exercise

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Equipment.BARBELL
import io.github.wtfjb.aximo.domain.model.Equipment.BODYWEIGHT
import io.github.wtfjb.aximo.domain.model.Equipment.CABLE
import io.github.wtfjb.aximo.domain.model.Equipment.DUMBBELL
import io.github.wtfjb.aximo.domain.model.Equipment.MACHINE
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.MuscleGroup.ABS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.ADDUCTORS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.BICEPS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.CALVES
import io.github.wtfjb.aximo.domain.model.MuscleGroup.CHEST
import io.github.wtfjb.aximo.domain.model.MuscleGroup.FOREARMS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.GLUTES
import io.github.wtfjb.aximo.domain.model.MuscleGroup.HAMSTRINGS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.LATS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.LOWER_BACK
import io.github.wtfjb.aximo.domain.model.MuscleGroup.QUADS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.SHOULDERS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.TRICEPS
import io.github.wtfjb.aximo.domain.model.MuscleGroup.UPPER_BACK
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import kotlinx.coroutines.flow.first

/**
 * Common strength and bodyweight exercises, created once so a new installation
 * isn't empty. Identified by `catalogId` = "strength_" + the lower-case name.
 * Display names come from the app's string resources (device language at the time
 * of creation); afterwards they are normal exercises the user can edit or archive.
 */
enum class CatalogExercise(
    val type: ExerciseType,
    val equipment: Equipment,
    val primary: Set<MuscleGroup>,
    val secondary: Set<MuscleGroup>,
    val repMin: Int,
    val repMax: Int,
) {
    // Chest
    BENCH_PRESS(ExerciseType.STRENGTH, BARBELL, setOf(CHEST), setOf(TRICEPS, SHOULDERS), 6, 10),
    INCLINE_BENCH_PRESS(ExerciseType.STRENGTH, BARBELL, setOf(CHEST), setOf(SHOULDERS, TRICEPS), 6, 10),
    DUMBBELL_BENCH_PRESS(ExerciseType.STRENGTH, DUMBBELL, setOf(CHEST), setOf(TRICEPS, SHOULDERS), 8, 12),
    INCLINE_DUMBBELL_PRESS(ExerciseType.STRENGTH, DUMBBELL, setOf(CHEST), setOf(SHOULDERS, TRICEPS), 8, 12),
    CABLE_FLY(ExerciseType.STRENGTH, CABLE, setOf(CHEST), setOf(SHOULDERS), 10, 15),
    PUSH_UP(ExerciseType.BODYWEIGHT, BODYWEIGHT, setOf(CHEST), setOf(TRICEPS, SHOULDERS), 8, 20),
    DIP(ExerciseType.BODYWEIGHT, BODYWEIGHT, setOf(CHEST, TRICEPS), setOf(SHOULDERS), 6, 12),

    // Back
    PULL_UP(ExerciseType.BODYWEIGHT, BODYWEIGHT, setOf(LATS, UPPER_BACK), setOf(BICEPS), 5, 10),
    CHIN_UP(ExerciseType.BODYWEIGHT, BODYWEIGHT, setOf(LATS), setOf(BICEPS, UPPER_BACK), 5, 10),
    LAT_PULLDOWN(ExerciseType.STRENGTH, CABLE, setOf(LATS), setOf(BICEPS, UPPER_BACK), 8, 12),
    BARBELL_ROW(ExerciseType.STRENGTH, BARBELL, setOf(UPPER_BACK, LATS), setOf(BICEPS, LOWER_BACK), 6, 10),
    DUMBBELL_ROW(ExerciseType.STRENGTH, DUMBBELL, setOf(LATS, UPPER_BACK), setOf(BICEPS), 8, 12),
    SEATED_CABLE_ROW(ExerciseType.STRENGTH, CABLE, setOf(UPPER_BACK, LATS), setOf(BICEPS), 8, 12),
    DEADLIFT(ExerciseType.STRENGTH, BARBELL, setOf(HAMSTRINGS, GLUTES, LOWER_BACK), setOf(UPPER_BACK, FOREARMS, QUADS), 3, 6),
    FACE_PULL(ExerciseType.STRENGTH, CABLE, setOf(SHOULDERS), setOf(UPPER_BACK), 12, 20),

    // Legs
    SQUAT(ExerciseType.STRENGTH, BARBELL, setOf(QUADS, GLUTES), setOf(ADDUCTORS, LOWER_BACK), 5, 8),
    FRONT_SQUAT(ExerciseType.STRENGTH, BARBELL, setOf(QUADS), setOf(GLUTES, ABS), 5, 8),
    LEG_PRESS(ExerciseType.STRENGTH, MACHINE, setOf(QUADS, GLUTES), setOf(ADDUCTORS), 8, 12),
    ROMANIAN_DEADLIFT(ExerciseType.STRENGTH, BARBELL, setOf(HAMSTRINGS, GLUTES), setOf(LOWER_BACK), 6, 10),
    BULGARIAN_SPLIT_SQUAT(ExerciseType.STRENGTH, DUMBBELL, setOf(QUADS, GLUTES), setOf(ADDUCTORS), 8, 12),
    LUNGE(ExerciseType.STRENGTH, DUMBBELL, setOf(QUADS, GLUTES), setOf(HAMSTRINGS), 8, 12),
    LEG_EXTENSION(ExerciseType.STRENGTH, MACHINE, setOf(QUADS), emptySet(), 10, 15),
    LEG_CURL(ExerciseType.STRENGTH, MACHINE, setOf(HAMSTRINGS), setOf(CALVES), 10, 15),
    HIP_THRUST(ExerciseType.STRENGTH, BARBELL, setOf(GLUTES), setOf(HAMSTRINGS), 8, 12),
    CALF_RAISE(ExerciseType.STRENGTH, MACHINE, setOf(CALVES), emptySet(), 10, 15),

    // Shoulders
    OVERHEAD_PRESS(ExerciseType.STRENGTH, BARBELL, setOf(SHOULDERS), setOf(TRICEPS), 5, 8),
    DUMBBELL_SHOULDER_PRESS(ExerciseType.STRENGTH, DUMBBELL, setOf(SHOULDERS), setOf(TRICEPS), 8, 12),
    LATERAL_RAISE(ExerciseType.STRENGTH, DUMBBELL, setOf(SHOULDERS), emptySet(), 12, 20),
    REVERSE_FLY(ExerciseType.STRENGTH, DUMBBELL, setOf(SHOULDERS), setOf(UPPER_BACK), 12, 20),

    // Arms
    BARBELL_CURL(ExerciseType.STRENGTH, BARBELL, setOf(BICEPS), setOf(FOREARMS), 8, 12),
    DUMBBELL_CURL(ExerciseType.STRENGTH, DUMBBELL, setOf(BICEPS), setOf(FOREARMS), 8, 12),
    HAMMER_CURL(ExerciseType.STRENGTH, DUMBBELL, setOf(BICEPS), setOf(FOREARMS), 8, 12),
    TRICEPS_PUSHDOWN(ExerciseType.STRENGTH, CABLE, setOf(TRICEPS), emptySet(), 10, 15),
    SKULL_CRUSHER(ExerciseType.STRENGTH, BARBELL, setOf(TRICEPS), emptySet(), 8, 12),
    OVERHEAD_TRICEPS_EXTENSION(ExerciseType.STRENGTH, CABLE, setOf(TRICEPS), emptySet(), 10, 15),

    // Core
    HANGING_LEG_RAISE(ExerciseType.BODYWEIGHT, BODYWEIGHT, setOf(ABS), emptySet(), 8, 15),
    CABLE_CRUNCH(ExerciseType.STRENGTH, CABLE, setOf(ABS), emptySet(), 10, 15),
    AB_WHEEL(ExerciseType.BODYWEIGHT, Equipment.OTHER, setOf(ABS), setOf(LOWER_BACK), 8, 12),
    ;

    val catalogId: String get() = "strength_" + name.lowercase()
}

object ExerciseCatalog {
    /**
     * The catalog is created only while there is no strength or bodyweight exercise
     * at all (archived ones included), so it never comes back after the user changed it.
     */
    fun needsSeed(allExercises: List<Exercise>): Boolean = allExercises.none { it.type != ExerciseType.CARDIO }

    /** Catalog exercises with names from [name]; increment and rest from the settings. */
    fun exercises(settings: TrainingSettings, name: (CatalogExercise) -> String): List<Exercise> =
        CatalogExercise.entries.map { entry ->
            Exercise(
                name = name(entry),
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
        }
}

/** Creates the catalog on first start (see [ExerciseCatalog.needsSeed]). */
class CatalogSeeder(
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    private val name: (CatalogExercise) -> String,
) {
    suspend fun seedIfEmpty() {
        if (!ExerciseCatalog.needsSeed(exercises.observeExercises(includeArchived = true).first())) return
        ExerciseCatalog.exercises(settings.training.first(), name).forEach { exercises.saveExercise(it) }
    }

    /**
     * Adds the catalog exercises that don't exist yet (by catalog id, archived ones
     * count as existing), e.g. for an installation that started before the catalog.
     * Returns how many were added.
     */
    suspend fun addMissing(): Int {
        val existing = exercises.observeExercises(includeArchived = true).first().mapNotNull { it.catalogId }.toSet()
        val missing = ExerciseCatalog.exercises(settings.training.first(), name).filter { it.catalogId !in existing }
        missing.forEach { exercises.saveExercise(it) }
        return missing.size
    }
}
