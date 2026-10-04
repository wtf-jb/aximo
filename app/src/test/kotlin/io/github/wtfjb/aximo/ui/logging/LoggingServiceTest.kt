package io.github.wtfjb.aximo.ui.logging

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.logging.LoggingException
import io.github.wtfjb.aximo.domain.logging.LoggingInput
import io.github.wtfjb.aximo.domain.logging.LoggingService
import io.github.wtfjb.aximo.domain.logging.ParsedExercise
import io.github.wtfjb.aximo.domain.logging.ParsedLog
import io.github.wtfjb.aximo.domain.logging.ParsedSetGroup
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import io.github.wtfjb.aximo.ui.ai.FakeAiProfileRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import io.github.wtfjb.aximo.ui.workout.FakeProgressionRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LoggingServiceTest {

    private val now = Instant.fromEpochSeconds(1_700_000_000)
    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val squat = Exercise(id = 2, name = "Kniebeuge", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val cardio = Exercise(id = 3, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)

    private val exercises = FakeExerciseRepository(listOf(bench, squat, cardio))
    private val workouts = FakeWorkoutRepository({ id -> exercises.all.first { it.id == id } })
    private val profiles = FakeAiProfileRepository()
    private val settings = FakeSettingsRepository()
    private val time = TimeSource { now }
    private val starter = WorkoutStarter(workouts, FakeRoutineRepository(), FakeProgressionRepository(), time)

    private var catalog = emptyList<CatalogEntry>()
    private var answer = ParsedLog(emptyList())
    private var sentInput: LoggingInput? = null
    private var sentLanguage: String? = null
    private var usedKey: String? = null

    private val service = LoggingService(
        exercises = exercises,
        workouts = workouts,
        starter = starter,
        settings = settings,
        catalog = { catalog },
        profiles = profiles,
        factory = { _, key ->
            usedKey = key
            object : AiProvider {
                override suspend fun complete(request: AiRequest): String = error("not used, the generator is faked")
            }
        },
        generator = { _, input, language ->
            sentInput = input
            sentLanguage = language
            answer
        },
        time = time,
    )

    private fun group(count: Int = 1, weight: Double? = 80.0, reps: Int = 8, rir: Int? = null, type: SetType = SetType.WORKING) =
        ParsedSetGroup(count, weight, null, reps, rir, null, type)

    private suspend fun withProfile() {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Set("geheim"))
    }

    private suspend fun expectError(text: String): LoggingException.Reason {
        try {
            service.parse(text, "de")
        } catch (e: LoggingException) {
            return e.reason
        }
        fail("Expected LoggingException")
        throw AssertionError()
    }

    @Test
    fun needsProfileAndText() = runTest {
        assertEquals(LoggingException.Reason.EMPTY_TEXT, expectError("  "))
        assertEquals(LoggingException.Reason.NO_PROFILE, expectError("Bankdrücken 3x8"))
        withProfile()
        answer = ParsedLog(emptyList())
        assertEquals(LoggingException.Reason.NOTHING_USABLE, expectError("Hallo"))
    }

    @Test
    fun parseSendsTextAndNamesOnlyAndSavesNothing() = runTest {
        withProfile()
        answer = ParsedLog(listOf(ParsedExercise("Bankdrücken", null, null, listOf(group(count = 3, rir = 2)))))

        val proposal = service.parse(" Bankdrücken 3x8 80 kg RIR 2 ", "de-DE")

        assertEquals(3, proposal.entries.single().sets.size)
        assertEquals("geheim", usedKey)
        assertEquals("de-DE", sentLanguage)
        assertEquals("Bankdrücken 3x8 80 kg RIR 2", sentInput!!.text)
        // No cardio exercise, nothing but the names.
        assertEquals(listOf("Bankdrücken", "Kniebeuge"), sentInput!!.exercises.map { it.name })
        assertNull(workouts.current)
    }

    @Test
    fun saveLogsDoneSetsInTheRunningWorkout() = runTest {
        val workoutId = starter.startOrResume()
        withProfile()
        answer = ParsedLog(
            listOf(
                ParsedExercise("Bankdrücken", null, null, listOf(group(count = 2, rir = 2), group(weight = 40.0, reps = 10, type = SetType.WARM_UP))),
                ParsedExercise("Kniebeuge", null, null, listOf(group(weight = null))),
            ),
        )
        val proposal = service.parse("x", "de")

        val result = service.save(proposal)

        assertEquals(Triple(2, 4, 0), Triple(result.exercises, result.sets, result.created))
        val workout = workouts.current!!
        assertEquals(workoutId, workout.workout.id)
        assertEquals(listOf(1L, 2L), workout.exercises.map { it.entry.exerciseId })
        val sets = workout.exercises[0].sets
        assertEquals(listOf(80.0, 80.0, 40.0), sets.map { it.weightKg })
        assertEquals(listOf(2, 2, null), sets.map { it.rir })
        assertEquals(SetType.WARM_UP, sets[2].setType)
        assertTrue(sets.all { it.completedAt == now })
        // No weight in the text: saved as 0 kg.
        assertEquals(0.0, workout.exercises[1].sets.single().weightKg, 0.0)
    }

    @Test
    fun saveStartsAFreeWorkoutIfNoneIsRunning() = runTest {
        withProfile()
        answer = ParsedLog(listOf(ParsedExercise("Bankdrücken", null, null, listOf(group()))))
        val proposal = service.parse("x", "de")
        assertNull(workouts.current)

        service.save(proposal)

        assertEquals(null, workouts.current!!.workout.routineId)
        assertEquals(1, workouts.current!!.exercises.size)
    }

    @Test
    fun saveStoresRpeAndConvertedWeight() = runTest {
        settings.setTraining(TrainingSettings().withUnit(WeightUnit.LBS))
        withProfile()
        answer = ParsedLog(listOf(ParsedExercise("Bankdrücken", null, null, listOf(ParsedSetGroup(1, 225.0, null, 5, null, 8.5, SetType.WORKING)))))

        service.save(service.parse("x", "de"))

        val set = workouts.current!!.exercises.single().sets.single()
        assertEquals(225 * WeightUnit.KG_PER_LB, set.weightKg, 1e-9)
        assertEquals(8.5, set.rpe!!, 0.0)
        assertNull(set.rir)
    }

    @Test
    fun saveCreatesNewExercisesOnlyWhenConfirmed() = runTest {
        withProfile()
        catalog = listOf(
            CatalogEntry("Cable_Pullover", "Cable Pullover", ExerciseType.STRENGTH, Equipment.CABLE, setOf(MuscleGroup.LATS), emptySet(), 10, 15, listOf("Pull.")),
        )
        answer = ParsedLog(
            listOf(
                ParsedExercise("Cable Pullover", null, null, listOf(group(reps = 12))),
                ParsedExercise("Cable Pullover", null, null, listOf(group(reps = 10))),
                ParsedExercise("Wadenheben", null, Equipment.MACHINE, listOf(group())),
            ),
        )
        val proposal = service.parse("x", "de")
        val before = exercises.all.size

        // Not saved: nothing is created.
        service.save(proposal.copy(entries = proposal.entries.map { it.copy(included = false) }))
        assertEquals(before, exercises.all.size)
        assertNull(workouts.current)

        val result = service.save(proposal.setIncluded(2, false))

        assertEquals(1, result.created) // the same new name twice is one exercise
        assertEquals(2, result.exercises)
        val created = exercises.all.single { it.name == "Cable Pullover" }
        assertEquals("fed_Cable_Pullover", created.catalogId)
        assertEquals(setOf(MuscleGroup.LATS), created.primaryMuscles)
        assertEquals(listOf(created.id, created.id), workouts.current!!.exercises.map { it.entry.exerciseId })
        assertEquals(before + 1, exercises.all.size)
    }

    @Test
    fun saveSkipsEntriesWithoutAChoice() = runTest {
        withProfile()
        exercises.setArchived(1, true)
        exercises.saveExercise(bench.copy(id = 0, name = "Bankdrücken Kurzhantel"))
        exercises.saveExercise(bench.copy(id = 0, name = "Bankdrücken Langhantel"))
        answer = ParsedLog(listOf(ParsedExercise("Bankdrücken", null, null, listOf(group()))))
        val proposal = service.parse("x", "de")
        assertTrue(proposal.needsChoice)

        val result = service.save(proposal)

        assertEquals(0, result.sets)
        assertNull(workouts.current)
    }

    @Test
    fun inputMatchesWhatParseSends() = runTest {
        withProfile()
        answer = ParsedLog(listOf(ParsedExercise("Bankdrücken", null, null, listOf(group()))))

        val input = service.input("Text")
        service.parse("Text", "de")

        assertEquals(input, sentInput)
    }
}
