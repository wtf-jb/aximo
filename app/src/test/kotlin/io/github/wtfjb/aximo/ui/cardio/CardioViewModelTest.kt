package io.github.wtfjb.aximo.ui.cardio

import io.github.wtfjb.aximo.domain.cardio.CardioFieldError
import io.github.wtfjb.aximo.domain.cardio.DefaultActivity
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CardioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now = Instant.fromEpochSeconds(1_790_000_037)
    private val time = TimeSource { now }
    private val names: (DefaultActivity) -> String = { it.name.lowercase() }
    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)

    private fun vm(
        exercises: FakeExerciseRepository,
        cardio: FakeCardioRepository,
        entryId: Long = 0,
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ) = CardioViewModel(cardio, exercises, time, settings, names, entryId)

    @Test
    fun estimatesCaloriesOnlyWithBodyWeightAndDuration() = runTest {
        val without = vm(FakeExerciseRepository(), FakeCardioRepository())
        without.onDurationChange("", "50", "")
        without.onDistanceChange("10")
        assertNull(without.uiState.value.kcal)

        val with = vm(FakeExerciseRepository(), FakeCardioRepository(), settings = FakeSettingsRepository(TrainingSettings(bodyWeightKg = 80.0)))
        assertNull(with.uiState.value.kcal)
        with.onDurationChange("", "50", "")
        with.onDistanceChange("10")
        // Running preselected, 12 km/h: MET ≈ 11.44 × 3.5 × 80 / 200 × 50 min
        assertEquals(800, with.uiState.value.kcal)
    }

    @Test
    fun firstUseCreatesDefaultActivitiesAndPreselectsRunning() = runTest {
        val exercises = FakeExerciseRepository(listOf(bench))
        val state = vm(exercises, FakeCardioRepository()).uiState.value

        assertEquals(listOf("running", "cycling", "rowing", "other"), state.activities.map { it.name })
        assertEquals(5, exercises.all.size)
        assertEquals(state.activities[0].id, state.draft.exerciseId)
        assertEquals(PaceStyle.PER_KM, state.paceStyle)
        // Start time defaults to now, to the minute.
        assertEquals(1_789_999_980L, state.draft.startedAt.epochSeconds)
    }

    @Test
    fun existingCardioExercisesAreNotDuplicated() = runTest {
        val swim = Exercise(id = 2, name = "Schwimmen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)
        val exercises = FakeExerciseRepository(listOf(bench, swim))
        val state = vm(exercises, FakeCardioRepository()).uiState.value

        assertEquals(listOf(swim), state.activities)
        assertEquals(2, exercises.all.size)
    }

    @Test
    fun savingNeedsDurationAndStoresEntry() = runTest {
        val cardio = FakeCardioRepository()
        val vm = vm(FakeExerciseRepository(), cardio)

        vm.save()
        assertEquals(setOf(CardioFieldError.DURATION_MISSING), vm.uiState.value.errors)
        assertTrue(cardio.all.isEmpty())

        vm.onDurationChange("", "28", "41")
        vm.onDistanceChange("5,2")
        vm.onHeartRateChange("152")
        vm.onNoteChange("locker")
        assertEquals("5:31", io.github.wtfjb.aximo.domain.cardio.CardioMath.formatPace(vm.uiState.value.paceSeconds!!))
        vm.save()

        val saved = cardio.all.single()
        assertEquals(1721, saved.durationSec)
        assertEquals(5200.0, saved.distanceM!!, 0.001)
        assertEquals(152, saved.avgHeartRate)
        assertEquals("locker", saved.note)
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun invalidInputKeepsTheOldValueAndBlankClears() = runTest {
        val vm = vm(FakeExerciseRepository(), FakeCardioRepository())
        vm.onDistanceChange("5")
        vm.onDistanceChange("abc")
        assertEquals(5000.0, vm.uiState.value.draft.distanceM!!, 0.001)
        vm.onDistanceChange(" ")
        assertNull(vm.uiState.value.draft.distanceM)

        vm.onHeartRateChange("3")
        assertNull(vm.uiState.value.draft.avgHeartRate)
        vm.onDurationChange("0", "0", "0")
        assertNull(vm.uiState.value.draft.durationSec)
    }

    @Test
    fun newEntryPreselectsTheLastUsedActivity() = runTest {
        val exercises = FakeExerciseRepository()
        val first = vm(exercises, FakeCardioRepository()).uiState.value
        val rowing = first.activities[2]
        val cardio = FakeCardioRepository(listOf(CardioEntry(id = 1, exerciseId = rowing.id, startedAt = now, durationSec = 600)))

        val state = vm(exercises, cardio).uiState.value

        assertEquals(rowing.id, state.draft.exerciseId)
        assertEquals(PaceStyle.PER_500M, state.paceStyle)
    }

    @Test
    fun editingUpdatesAndDeleteRemoves() = runTest {
        val run = Exercise(id = 2, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER, archived = true)
        val entry = CardioEntry(id = 7, exerciseId = run.id, startedAt = now, durationSec = 600)
        val cardio = FakeCardioRepository(listOf(entry))
        val exercises = FakeExerciseRepository(listOf(run))

        val vm = vm(exercises, cardio, entryId = 7)
        assertFalse(vm.uiState.value.isNew)
        // The archived activity of the entry stays selectable.
        assertEquals(listOf(run), vm.uiState.value.activities)

        vm.onDurationChange("", "15", "")
        vm.save()
        assertEquals(entry.copy(durationSec = 900), cardio.all.single())

        val again = vm(exercises, cardio, entryId = 7)
        again.delete()
        assertTrue(cardio.all.isEmpty())
        assertTrue(again.uiState.value.done)
    }

    @Test
    fun missingEntryClosesTheScreen() = runTest {
        assertTrue(vm(FakeExerciseRepository(), FakeCardioRepository(), entryId = 99).uiState.value.done)
    }
}
