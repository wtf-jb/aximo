package io.github.wtfjb.aximo.data.backup

import androidx.room.withTransaction
import io.github.wtfjb.aximo.data.db.AximoDatabase
import io.github.wtfjb.aximo.data.db.entity.BlockEntity
import io.github.wtfjb.aximo.data.db.entity.CardioEntryEntity
import io.github.wtfjb.aximo.data.db.entity.CycleEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseMuscleEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseWithMuscles
import io.github.wtfjb.aximo.data.db.entity.ProgressionStateEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.SetEntryEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutExerciseEntity
import io.github.wtfjb.aximo.data.repository.toDomain
import io.github.wtfjb.aximo.domain.backup.BackupException
import io.github.wtfjb.aximo.domain.backup.BackupRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Export and restore of the whole database as [BackupFile] JSON (A-08). */
class RoomBackupRepository(
    private val db: AximoDatabase,
    private val settings: SettingsRepository,
    private val time: TimeSource,
) : BackupRepository {

    private val dao = db.backupDao()

    private val json = Json {
        prettyPrint = true
        // Fields added by newer versions of the same schema are skipped instead of failing.
        ignoreUnknownKeys = true
    }

    override suspend fun exportJson(): String {
        val file = db.withTransaction {
            BackupFile(
                schemaVersion = BackupFile.CURRENT_SCHEMA_VERSION,
                exportedAtMillis = time.now().toEpochMilliseconds(),
                exercises = dao.exercises().map { it.toRow() },
                exerciseMuscles = dao.exerciseMuscles().map { ExerciseMuscleRow(it.exerciseId, it.muscleGroup.name, it.role.name) },
                cycles = dao.cycles().map { CycleRow(it.id, it.name, it.startDate.toEpochDays(), it.weeks) },
                blocks = dao.blocks().map { BlockRow(it.id, it.cycleId, it.name, it.position, it.weeks, it.isDeload) },
                routines = dao.routines().map { RoutineRow(it.id, it.name, it.position, it.cycleId) },
                routineExercises = dao.routineExercises().map { it.toRow() },
                workouts = dao.workouts().map { it.toRow() },
                workoutExercises = dao.workoutExercises().map { WorkoutExerciseRow(it.id, it.workoutId, it.exerciseId, it.position, it.supersetGroup, it.note) },
                sets = dao.sets().map { it.toRow() },
                cardioEntries = dao.cardioEntries().map { it.toRow() },
                progression = dao.progression().map { ProgressionRow(it.exerciseId, it.nextWeightKg, it.nextRepTarget, it.reason.name) },
                settings = settings.training.first().toRow(),
            )
        }
        return json.encodeToString(BackupFile.serializer(), file)
    }

    override suspend fun restoreJson(json: String) {
        val rows = invalidFileOnError { toEntities(parse(json)) }
        // Broken references (e.g. a set without its workout) fail here; the transaction is rolled back.
        invalidFileOnError {
            db.withTransaction {
                dao.deleteAll()
                dao.insertExercises(rows.exercises)
                dao.insertExerciseMuscles(rows.exerciseMuscles)
                dao.insertCycles(rows.cycles)
                dao.insertBlocks(rows.blocks)
                dao.insertRoutines(rows.routines)
                dao.insertRoutineExercises(rows.routineExercises)
                dao.insertWorkouts(rows.workouts)
                dao.insertWorkoutExercises(rows.workoutExercises)
                dao.insertSets(rows.sets)
                dao.insertCardioEntries(rows.cardioEntries)
                dao.insertProgression(rows.progression)
            }
        }
        rows.settings?.let { settings.setTraining(it) }
    }

    /**
     * Converts the rows to database entities and checks them with the domain rules
     * (e.g. no blank names, valid rep ranges), so the app never reads data it would
     * reject. Throws on unknown enum names or invalid values.
     */
    private fun toEntities(file: BackupFile): RestoreRows {
        val rows = RestoreRows(
            exercises = file.exercises.map { it.toEntity() },
            exerciseMuscles = file.exerciseMuscles.map { ExerciseMuscleEntity(it.exerciseId, enumValueOf(it.muscleGroup), enumValueOf(it.role)) },
            cycles = file.cycles.map { CycleEntity(it.id, it.name, LocalDate.fromEpochDays(it.startEpochDay), it.weeks) },
            blocks = file.blocks.map { BlockEntity(it.id, it.cycleId, it.name, it.position, it.weeks, it.isDeload) },
            routines = file.routines.map { RoutineEntity(it.id, it.name, it.position, it.cycleId) },
            routineExercises = file.routineExercises.map { it.toEntity() },
            workouts = file.workouts.map { it.toEntity() },
            workoutExercises = file.workoutExercises.map { WorkoutExerciseEntity(it.id, it.workoutId, it.exerciseId, it.position, it.supersetGroup, it.note) },
            sets = file.sets.map { it.toEntity() },
            cardioEntries = file.cardioEntries.map { it.toEntity() },
            progression = file.progression.map { ProgressionStateEntity(it.exerciseId, it.nextWeightKg, it.nextRepTarget, enumValueOf(it.reason)) },
            settings = file.settings?.toSettings(),
        )
        val musclesByExercise = rows.exerciseMuscles.groupBy { it.exerciseId }
        rows.exercises.forEach { ExerciseWithMuscles(it, musclesByExercise[it.id].orEmpty()).toDomain() }
        rows.routines.forEach { it.toDomain() }
        rows.routineExercises.forEach { it.toDomain() }
        rows.workouts.forEach { it.toDomain() }
        rows.sets.forEach { it.toDomain() }
        rows.cardioEntries.forEach { it.toDomain() }
        return rows
    }

    private suspend fun <T> invalidFileOnError(block: suspend () -> T): T = try {
        block()
    } catch (e: BackupException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw BackupException(BackupException.Reason.INVALID_FILE, e)
    }

    /** Checks the version first, so a newer file gets a clear message instead of a parse error. */
    private fun parse(text: String): BackupFile {
        val version = try {
            json.parseToJsonElement(text).jsonObject.getValue("schemaVersion").jsonPrimitive.int
        } catch (e: Exception) {
            throw BackupException(BackupException.Reason.INVALID_FILE, e)
        }
        if (version > BackupFile.CURRENT_SCHEMA_VERSION) throw BackupException(BackupException.Reason.NEWER_VERSION)
        if (version < 1) throw BackupException(BackupException.Reason.INVALID_FILE)
        return json.decodeFromString(BackupFile.serializer(), text)
    }
}

/** Everything a restore writes, already converted and checked. */
private class RestoreRows(
    val exercises: List<ExerciseEntity>,
    val exerciseMuscles: List<ExerciseMuscleEntity>,
    val cycles: List<CycleEntity>,
    val blocks: List<BlockEntity>,
    val routines: List<RoutineEntity>,
    val routineExercises: List<RoutineExerciseEntity>,
    val workouts: List<WorkoutEntity>,
    val workoutExercises: List<WorkoutExerciseEntity>,
    val sets: List<SetEntryEntity>,
    val cardioEntries: List<CardioEntryEntity>,
    val progression: List<ProgressionStateEntity>,
    val settings: TrainingSettings?,
)

private fun ExerciseEntity.toRow() = ExerciseRow(
    id, name, type.name, equipment.name, note, incrementKg, repRangeMin, repRangeMax, restSeconds, roundingStepKg, catalogId, archived,
)

private fun ExerciseRow.toEntity() = ExerciseEntity(
    id, name, enumValueOf(type), enumValueOf(equipment), note, incrementKg, repRangeMin, repRangeMax, restSeconds, roundingStepKg, catalogId, archived,
)

private fun RoutineExerciseEntity.toRow() =
    RoutineExerciseRow(id, routineId, exerciseId, position, targetSets, repMin, repMax, targetRir, supersetGroup)

private fun RoutineExerciseRow.toEntity() =
    RoutineExerciseEntity(id, routineId, exerciseId, position, targetSets, repMin, repMax, targetRir, supersetGroup)

private fun WorkoutEntity.toRow() =
    WorkoutRow(id, startedAt.toEpochMilliseconds(), endedAt?.toEpochMilliseconds(), routineId, note, rating)

private fun WorkoutRow.toEntity() = WorkoutEntity(
    id, Instant.fromEpochMilliseconds(startedAtMillis), endedAtMillis?.let { Instant.fromEpochMilliseconds(it) }, routineId, note, rating,
)

private fun SetEntryEntity.toRow() =
    SetRow(id, workoutExerciseId, position, weightKg, reps, rpe, rir, setType.name, completedAt?.toEpochMilliseconds())

private fun SetRow.toEntity() = SetEntryEntity(
    id, workoutExerciseId, position, weightKg, reps, rpe, rir, enumValueOf(setType), completedAtMillis?.let { Instant.fromEpochMilliseconds(it) },
)

private fun CardioEntryEntity.toRow() = CardioRow(
    id, workoutId, exerciseId, startedAt.toEpochMilliseconds(), durationSec, distanceM, avgHeartRate, elevationM, note, source.name, externalId,
)

private fun CardioRow.toEntity() = CardioEntryEntity(
    id, workoutId, exerciseId, Instant.fromEpochMilliseconds(startedAtMillis), durationSec, distanceM, avgHeartRate, elevationM, note,
    enumValueOf(source), externalId,
)

private fun TrainingSettings.toRow() = SettingsRow(unit.name, restSeconds, steps.barbellKg, steps.dumbbellKg, rating.name, weeklyGoal)

/** Invalid settings in a file are skipped; the data is what matters. */
private fun SettingsRow.toSettings(): TrainingSettings? = runCatching {
    TrainingSettings(
        unit = enumValueOf<WeightUnit>(weightUnit),
        restSeconds = restSeconds,
        steps = WeightSteps(stepBarbellKg, stepDumbbellKg),
        rating = enumValueOf<SetRating>(setRating),
        weeklyGoal = weeklyGoal,
    )
}.getOrNull()
