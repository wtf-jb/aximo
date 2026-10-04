package io.github.wtfjb.aximo.data.backup

import kotlinx.serialization.Serializable

/**
 * The JSON backup format (A-08). It mirrors the database tables one to one, ids
 * included, so a restore gives back exactly the same data. Times are epoch
 * milliseconds (UTC), dates are epoch days, enums are their names, weights kg.
 *
 * [schemaVersion] goes up whenever the format changes. A file with a higher
 * version than [CURRENT_SCHEMA_VERSION] is refused; older versions are converted
 * when they exist.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val exercises: List<ExerciseRow> = emptyList(),
    val exerciseMuscles: List<ExerciseMuscleRow> = emptyList(),
    val cycles: List<CycleRow> = emptyList(),
    val blocks: List<BlockRow> = emptyList(),
    val routines: List<RoutineRow> = emptyList(),
    val routineExercises: List<RoutineExerciseRow> = emptyList(),
    val workouts: List<WorkoutRow> = emptyList(),
    val workoutExercises: List<WorkoutExerciseRow> = emptyList(),
    val sets: List<SetRow> = emptyList(),
    val cardioEntries: List<CardioRow> = emptyList(),
    val progression: List<ProgressionRow> = emptyList(),
    val settings: SettingsRow? = null,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class ExerciseRow(
    val id: Long,
    val name: String,
    val type: String,
    val equipment: String,
    val note: String,
    val incrementKg: Double,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val restSeconds: Int,
    val roundingStepKg: Double? = null,
    val catalogId: String? = null,
    val archived: Boolean,
)

@Serializable
data class ExerciseMuscleRow(val exerciseId: Long, val muscleGroup: String, val role: String)

@Serializable
data class CycleRow(val id: Long, val name: String, val startEpochDay: Long, val weeks: Int)

@Serializable
data class BlockRow(val id: Long, val cycleId: Long, val name: String, val position: Int, val weeks: Int, val isDeload: Boolean)

@Serializable
data class RoutineRow(val id: Long, val name: String, val position: Int, val cycleId: Long? = null)

@Serializable
data class RoutineExerciseRow(
    val id: Long,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int? = null,
    val supersetGroup: String? = null,
)

@Serializable
data class WorkoutRow(
    val id: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val routineId: Long? = null,
    val note: String,
    val rating: Int? = null,
)

@Serializable
data class WorkoutExerciseRow(
    val id: Long,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val supersetGroup: String? = null,
    val note: String,
)

@Serializable
data class SetRow(
    val id: Long,
    val workoutExerciseId: Long,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null,
    val rir: Int? = null,
    val setType: String,
    val completedAtMillis: Long? = null,
)

@Serializable
data class CardioRow(
    val id: Long,
    val workoutId: Long? = null,
    val exerciseId: Long,
    val startedAtMillis: Long,
    val durationSec: Int,
    val distanceM: Double? = null,
    val avgHeartRate: Int? = null,
    val elevationM: Double? = null,
    val note: String,
    val source: String,
    val externalId: String? = null,
)

@Serializable
data class ProgressionRow(val exerciseId: Long, val nextWeightKg: Double, val nextRepTarget: Int, val reason: String)

/** Training settings (A-09); the theme stays a per-device choice and is not part of the backup. */
@Serializable
data class SettingsRow(val weightUnit: String, val restSeconds: Int, val stepBarbellKg: Double, val stepDumbbellKg: Double)
