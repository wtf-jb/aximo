package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.data.backup.RoomBackupRepository
import io.github.wtfjb.aximo.data.repository.RoomCardioRepository
import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.data.repository.RoomProgressionRepository
import io.github.wtfjb.aximo.data.repository.RoomRoutineRepository
import io.github.wtfjb.aximo.data.repository.RoomWorkoutRepository
import io.github.wtfjb.aximo.data.testdata.RoomDevDataRepository
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import io.github.wtfjb.aximo.domain.exercise.CatalogSeeder
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DevDataRepositoryTest : DatabaseTest() {

    private val now = Instant.parse("2026-10-04T12:00:00Z")
    private val settings = DevSettings()
    private lateinit var dev: RoomDevDataRepository
    private lateinit var exercises: RoomExerciseRepository
    private lateinit var workouts: RoomWorkoutRepository

    @Before
    fun setUp() {
        exercises = RoomExerciseRepository(db.exerciseDao())
        workouts = RoomWorkoutRepository(db.workoutDao())
        val backup = RoomBackupRepository(db, settings) { now }
        dev = RoomDevDataRepository(db, backup, CatalogSeeder(exercises, settings) { it.name }, { now }, { TimeZone.UTC })
    }

    @Test
    fun testDataCoversAboutThreeYearsOfTraining() = runTest {
        dev.loadTestData()

        val finished = workouts.observeFinished().first()
        assertTrue("workouts: ${finished.size}", finished.size in 300..600)
        val first = finished.minOf { it.workout.startedAt }
        val last = finished.maxOf { it.workout.startedAt }
        assertTrue(first < Instant.parse("2023-11-01T00:00:00Z"))
        assertTrue(last < now)
        assertTrue(finished.all { it.workout.endedAt != null })
        assertTrue(finished.any { it.workout.routineId == null })
        assertTrue(finished.any { it.workout.rating != null })
        val allSets = finished.flatMap { w -> w.exercises.flatMap { it.sets } }
        assertTrue(allSets.size > 5_000)
        assertTrue(SetType.entries.all { type -> allSets.any { it.setType == type } })
        assertTrue(finished.flatMap { it.exercises }.any { it.entry.supersetGroup != null })
    }

    @Test
    fun testDataHasRoutinesCardioAndProgression() = runTest {
        dev.loadTestData()

        val routines = RoomRoutineRepository(db.routineDao()).observeRoutinesWithExercises().first()
        assertEquals(listOf("Push", "Pull", "Legs"), routines.map { it.routine.name })
        assertTrue(routines.all { it.exercises.size >= 5 })
        val cardio = RoomCardioRepository(db.cardioDao()).observeAll().first()
        assertTrue(cardio.size > 100)
        assertTrue(cardio.any { it.workoutId != null })
        val cardioExercises = exercises.observeExercises(includeArchived = false).first().filter { it.type == ExerciseType.CARDIO }
        assertEquals(setOf("Laufen", "Radfahren", "Rudern"), cardioExercises.map { it.name }.toSet())
        val benchId = exercises.observeExercises(includeArchived = false).first().first { it.catalogId == CatalogExercise.BENCH_PRESS.catalogId }.id
        assertTrue(RoomProgressionRepository(db.progressionDao()).get(benchId) != null)
    }

    @Test
    fun loadingTwiceGivesTheSameDataNotDoubleTheData() = runTest {
        dev.loadTestData()
        val first = workouts.observeFinished().first().size

        dev.loadTestData()

        assertEquals(first, workouts.observeFinished().first().size)
    }

    @Test
    fun clearAllDeletesUserDataAndRestoresTheStandardExercises() = runTest {
        dev.loadTestData()

        dev.clearAll()

        assertEquals(emptyList<Any>(), workouts.observeFinished().first())
        assertEquals(emptyList<Any>(), RoomRoutineRepository(db.routineDao()).observeRoutines().first())
        assertEquals(emptyList<Any>(), RoomCardioRepository(db.cardioDao()).observeAll().first())
        val remaining = exercises.observeExercises(includeArchived = true).first()
        assertEquals(CatalogExercise.entries.size, remaining.size)
        assertTrue(remaining.none { it.type == ExerciseType.CARDIO })
    }
}

private class DevSettings : SettingsRepository {
    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val training = MutableStateFlow(TrainingSettings())

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override suspend fun setTraining(settings: TrainingSettings) {
        training.value = settings
    }
}
