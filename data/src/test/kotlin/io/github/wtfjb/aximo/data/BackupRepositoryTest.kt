package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.backup.RoomBackupRepository
import io.github.wtfjb.aximo.data.repository.RoomCardioRepository
import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.data.repository.RoomProgressionRepository
import io.github.wtfjb.aximo.data.repository.RoomRoutineRepository
import io.github.wtfjb.aximo.data.repository.RoomWorkoutRepository
import io.github.wtfjb.aximo.domain.backup.BackupException
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import io.github.wtfjb.aximo.domain.workout.SetRating
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest : DatabaseTest() {

    private val t0 = Instant.fromEpochSeconds(1_790_000_000)
    private val settings = InMemorySettings()
    private lateinit var backup: RoomBackupRepository
    private lateinit var exercises: RoomExerciseRepository
    private lateinit var routines: RoomRoutineRepository
    private lateinit var workouts: RoomWorkoutRepository
    private lateinit var cardio: RoomCardioRepository
    private lateinit var progression: RoomProgressionRepository

    @Before
    fun setUp() {
        backup = RoomBackupRepository(db, settings) { t0 }
        exercises = RoomExerciseRepository(db.exerciseDao())
        routines = RoomRoutineRepository(db.routineDao())
        workouts = RoomWorkoutRepository(db.workoutDao())
        cardio = RoomCardioRepository(db.cardioDao())
        progression = RoomProgressionRepository(db.progressionDao())
    }

    /** One of everything: exercises, a routine, a finished workout, cardio, a suggestion. */
    private suspend fun fillDatabase() {
        val benchId = exercises.saveExercise(benchPress())
        val runId = exercises.saveExercise(running())
        val routineId = routines.saveRoutine(Routine(name = "Push A"), listOf(RoutineExercise(exerciseId = benchId, position = 0, targetSets = 3, repMin = 6, repMax = 8, targetRir = 2)))
        val workoutId = workouts.startWorkout(t0, routineId)
        workouts.addExercise(workoutId, benchId, null, listOf(PlannedSet(40.0, 10, setType = SetType.WARM_UP), PlannedSet(82.5, 8, rir = 1)))
        workouts.finishWorkout(workoutId, t0 + 1.hours, "gut")
        cardio.saveEntry(CardioEntry(exerciseId = runId, startedAt = t0 + 2.hours, durationSec = 1800, distanceM = 5000.0, note = "locker"))
        progression.save(ProgressionState(benchId, 85.0, 6, ProgressionReason.INCREASE_WEIGHT))
        settings.setTraining(TrainingSettings(restSeconds = 150, rating = SetRating.RPE, weeklyGoal = 4).withUnit(WeightUnit.LBS))
    }

    @Test
    fun exportThenRestoreGivesBackTheSameData() = runTest {
        fillDatabase()
        val json = backup.exportJson()
        val before = snapshot()

        db.backupDao().deleteAll()
        settings.setTraining(TrainingSettings())
        backup.restoreJson(json)

        assertEquals(before, snapshot())
        assertEquals(TrainingSettings(restSeconds = 150, rating = SetRating.RPE, weeklyGoal = 4).withUnit(WeightUnit.LBS), settings.training.first())
        // Exporting again gives the same file: nothing got lost on the way.
        assertEquals(json, backup.exportJson())
    }

    @Test
    fun exportContainsTheSchemaVersion() = runTest {
        assertTrue(backup.exportJson().contains("\"schemaVersion\": 1"))
    }

    @Test
    fun restoreReplacesExistingData() = runTest {
        val emptyExport = backup.exportJson()
        fillDatabase()

        backup.restoreJson(emptyExport)

        assertEquals(emptyList<Any>(), exercises.observeExercises(includeArchived = true).first())
        assertEquals(emptyList<Any>(), workouts.observeFinished().first())
    }

    @Test
    fun newerSchemaVersionIsRefused() = runTest {
        assertReason(BackupException.Reason.NEWER_VERSION) { backup.restoreJson("""{"schemaVersion": 99, "exportedAtMillis": 0}""") }
    }

    @Test
    fun brokenFilesAreRefusedAndNothingChanges() = runTest {
        fillDatabase()
        val before = snapshot()
        val valid = backup.exportJson()

        assertReason(BackupException.Reason.INVALID_FILE) { backup.restoreJson("not json") }
        assertReason(BackupException.Reason.INVALID_FILE) { backup.restoreJson("""{"exercises": []}""") }
        // Unknown enum name.
        assertReason(BackupException.Reason.INVALID_FILE) { backup.restoreJson(valid.replace("\"WARM_UP\"", "\"STRETCH\"")) }
        // Breaks a domain rule (blank exercise name).
        assertReason(BackupException.Reason.INVALID_FILE) { backup.restoreJson(valid.replace("\"Bankdrücken\"", "\" \"")) }
        // A set pointing to a workout exercise that doesn't exist: fails in the transaction, which is rolled back.
        val broken = Regex("\"workoutExerciseId\": (\\d+)").replace(valid) { "\"workoutExerciseId\": 999" }
        assertReason(BackupException.Reason.INVALID_FILE) { backup.restoreJson(broken) }

        assertEquals(before, snapshot())
    }

    private suspend fun snapshot(): List<Any> = listOf(
        exercises.observeExercises(includeArchived = true).first(),
        routines.observeRoutinesWithExercises().first(),
        workouts.observeFinished().first(),
        cardio.observeAll().first(),
        listOfNotNull(progression.get(exercises.observeExercises(true).first().firstOrNull()?.id ?: 0)),
    )

    private suspend fun assertReason(reason: BackupException.Reason, block: suspend () -> Unit) {
        try {
            block()
            fail("expected BackupException $reason")
        } catch (e: BackupException) {
            assertEquals(reason, e.reason)
            assertNotNull(e.message)
        }
    }
}

private class InMemorySettings : SettingsRepository {
    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val training = MutableStateFlow(TrainingSettings())

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override suspend fun setTraining(settings: TrainingSettings) {
        training.value = settings
    }
}
