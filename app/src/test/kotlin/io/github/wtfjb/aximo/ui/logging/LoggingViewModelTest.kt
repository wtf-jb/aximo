package io.github.wtfjb.aximo.ui.logging

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.domain.logging.LoggingException
import io.github.wtfjb.aximo.domain.logging.LoggingInput
import io.github.wtfjb.aximo.domain.logging.LoggingService
import io.github.wtfjb.aximo.domain.logging.ParsedExercise
import io.github.wtfjb.aximo.domain.logging.ParsedLog
import io.github.wtfjb.aximo.domain.logging.ParsedSetGroup
import io.github.wtfjb.aximo.domain.logging.SpeechError
import io.github.wtfjb.aximo.domain.logging.SpeechEvent
import io.github.wtfjb.aximo.domain.logging.SpeechInput
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import io.github.wtfjb.aximo.ui.ai.FakeAiPreferences
import io.github.wtfjb.aximo.ui.ai.FakeAiProfileRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import io.github.wtfjb.aximo.ui.workout.FakeProgressionRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Speech input driven by the test: [events] are what the recognizer reports. */
private class FakeSpeechInput(private val available: Boolean = true) : SpeechInput {
    val events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 16)
    val languages = mutableListOf<String>()
    var starts = 0

    override fun isAvailable() = available

    override fun listen(languageTag: String): Flow<SpeechEvent> {
        languages += languageTag
        starts++
        return events.transformWhile { emit(it); it !is SpeechEvent.Result && it !is SpeechEvent.Failed }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoggingViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val squat = Exercise(id = 2, name = "Kniebeuge", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val exercises = FakeExerciseRepository(listOf(bench, squat))
    private val workouts = FakeWorkoutRepository({ id -> exercises.all.first { it.id == id } })
    private val profiles = FakeAiProfileRepository()
    private val preferences = FakeAiPreferences()
    private val time = TimeSource { Instant.fromEpochSeconds(1_700_000_000) }

    private var calls = 0
    private var failure: Exception? = null
    private var answer = ParsedLog(
        listOf(
            ParsedExercise("Bankdrücken", null, null, listOf(ParsedSetGroup(3, 80.0, null, 8, 2, null, SetType.WORKING))),
            ParsedExercise("Kniebeuge", null, null, listOf(ParsedSetGroup(1, 100.0, null, 5, null, null, SetType.WORKING))),
        ),
    )

    private val service = LoggingService(
        exercises = exercises,
        workouts = workouts,
        starter = WorkoutStarter(workouts, FakeRoutineRepository(), FakeProgressionRepository(), time),
        settings = FakeSettingsRepository(),
        catalog = { emptyList() },
        profiles = profiles,
        factory = { _, _ ->
            object : AiProvider {
                override suspend fun complete(request: AiRequest): String = error("not used, the generator is faked")
            }
        },
        generator = { _, _, _ ->
            calls++
            failure?.let { throw it }
            answer
        },
        time = time,
    )

    private val speech = FakeSpeechInput()

    private fun viewModel(speech: SpeechInput = this.speech) =
        LoggingViewModel(service, preferences, speech, { "de-DE" }, { "data: ${it.text}, ${it.exercises.size} exercises" })

    private suspend fun withProfile() {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
    }

    @Test
    fun microphoneIsHiddenWithoutOnDeviceRecognizer() = runTest(UnconfinedTestDispatcher()) {
        val unavailable = FakeSpeechInput(available = false)
        val vm = viewModel(unavailable)

        assertFalse(vm.uiState.value.speechAvailable)
        vm.startListening()

        assertEquals(0, unavailable.starts)
        assertFalse(vm.uiState.value.listening)
    }

    @Test
    fun dictationFillsTheFieldAndCanBeCorrected() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()
        assertTrue(vm.uiState.value.speechAvailable)
        vm.setText("Bankdrücken 3x8")

        vm.startListening()
        assertTrue(vm.uiState.value.listening)
        assertEquals(listOf("de-DE"), speech.languages)
        speech.events.emit(SpeechEvent.Listening)
        speech.events.emit(SpeechEvent.Partial("und Knie"))
        assertEquals("Bankdrücken 3x8 und Knie", vm.uiState.value.text)
        speech.events.emit(SpeechEvent.Result("und Kniebeuge 100 Kilo"))

        assertEquals("Bankdrücken 3x8 und Kniebeuge 100 Kilo", vm.uiState.value.text)
        assertFalse(vm.uiState.value.listening)
        vm.setText("Bankdrücken 3x8 und Kniebeuge 100 kg")
        assertEquals("Bankdrücken 3x8 und Kniebeuge 100 kg", vm.uiState.value.text)
    }

    @Test
    fun dictationIntoEmptyFieldHasNoLeadingSpace() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()

        vm.startListening()
        speech.events.emit(SpeechEvent.Result("Klimmzüge 4x10"))

        assertEquals("Klimmzüge 4x10", vm.uiState.value.text)
    }

    @Test
    fun stoppingKeepsWhatWasRecognizedSoFar() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()
        vm.startListening()
        speech.events.emit(SpeechEvent.Partial("Bankdrücken"))

        vm.stopListening()

        assertFalse(vm.uiState.value.listening)
        assertEquals("Bankdrücken", vm.uiState.value.text)
    }

    @Test
    fun typingWhileListeningStopsTheRecognizer() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()
        vm.startListening()

        vm.setText("Eigener Text")
        speech.events.emit(SpeechEvent.Partial("zu spät"))

        assertFalse(vm.uiState.value.listening)
        assertEquals("Eigener Text", vm.uiState.value.text)
    }

    @Test
    fun speechErrorsAreShown() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()

        vm.startListening()
        speech.events.emit(SpeechEvent.Failed(SpeechError.NOTHING_HEARD))
        assertEquals(SpeechError.NOTHING_HEARD, vm.uiState.value.speechError)
        assertFalse(vm.uiState.value.listening)

        vm.startListening()
        assertNull(vm.uiState.value.speechError)

        vm.stopListening()
        vm.speechPermissionDenied()
        assertEquals(SpeechError.PERMISSION, vm.uiState.value.speechError)
    }

    @Test
    fun emptyTextIsRejectedWithoutAiCall() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        val vm = viewModel()

        vm.requestParse()

        assertEquals(LoggingError.Logging(LoggingException.Reason.EMPTY_TEXT), vm.uiState.value.error)
        assertEquals(0, calls)
        assertFalse(vm.uiState.value.showNotice)
    }

    @Test
    fun firstCallAsksForTheNoticeThenSendsOnlyAfterAccepting() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        val vm = viewModel()
        vm.setText("Bankdrücken 3x8 80 kg RIR 2")

        vm.requestParse()
        assertTrue(vm.uiState.value.showNotice)
        assertEquals(0, calls)

        vm.dismissNotice()
        assertEquals(0, calls)
        assertFalse(preferences.accepted.value)

        vm.requestParse()
        vm.acceptNotice()

        assertTrue(preferences.accepted.value)
        assertEquals(1, calls)
        assertEquals(2, vm.uiState.value.proposal!!.entries.size)
        assertFalse(vm.uiState.value.running)
    }

    @Test
    fun laterCallsGoStraightToTheAi() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.acceptDataNotice()
        val vm = viewModel()
        vm.setText("Bankdrücken 3x8")

        vm.requestParse()

        assertFalse(vm.uiState.value.showNotice)
        assertNotNull(vm.uiState.value.proposal)
    }

    @Test
    fun sentDataShowsExactlyTheInput() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()
        vm.setText("Bankdrücken 3x8")

        vm.showSentData()
        assertEquals("data: Bankdrücken 3x8, 2 exercises", vm.uiState.value.sentData)
        vm.dismissSentData()

        assertNull(vm.uiState.value.sentData)
    }

    @Test
    fun errorsFromTheProviderAndTheBuilderAreShown() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.acceptDataNotice()
        val vm = viewModel()
        vm.setText("Bankdrücken 3x8")

        failure = AiException(AiException.Reason.UNAUTHORIZED, statusCode = 401)
        vm.requestParse()
        assertEquals(LoggingError.Ai(AiException.Reason.UNAUTHORIZED, 401, null), vm.uiState.value.error)
        assertNull(vm.uiState.value.proposal)
        assertFalse(vm.uiState.value.running)

        failure = null
        answer = ParsedLog(emptyList())
        vm.requestParse()
        assertEquals(LoggingError.Logging(LoggingException.Reason.NOTHING_USABLE), vm.uiState.value.error)

        // Typing clears the error.
        vm.setText("Bankdrücken 3x8 80 kg")
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun noProfileIsReported() = runTest(UnconfinedTestDispatcher()) {
        preferences.acceptDataNotice()
        val vm = viewModel()
        vm.setText("Bankdrücken 3x8")

        vm.requestParse()

        assertEquals(LoggingError.Logging(LoggingException.Reason.NO_PROFILE), vm.uiState.value.error)
    }

    @Test
    fun backFromPreviewKeepsTheText() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.acceptDataNotice()
        val vm = viewModel()
        vm.setText("Bankdrücken 3x8")
        vm.requestParse()

        vm.discard()

        assertNull(vm.uiState.value.proposal)
        assertEquals("Bankdrücken 3x8", vm.uiState.value.text)
        assertNull(workouts.current)
    }

    @Test
    fun savingNeedsConfirmationAndRespectsTheSelection() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.acceptDataNotice()
        val vm = viewModel()
        vm.setText("Bankdrücken 3x8, Kniebeuge 100 kg 5")
        vm.requestParse()
        // Parsing alone saves nothing.
        assertNull(workouts.current)

        vm.setIncluded(1, false)
        vm.save()

        assertNotNull(vm.uiState.value.saved)
        assertEquals(1, vm.uiState.value.saved!!.exercises)
        assertEquals(3, vm.uiState.value.saved!!.sets)
        assertEquals(listOf(1L), workouts.current!!.exercises.map { it.entry.exerciseId })
    }

    @Test
    fun savingWaitsForTheChoiceOfAnUnsureExercise() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.acceptDataNotice()
        exercises.saveExercise(bench.copy(id = 0, name = "Bankdrücken Langhantel"))
        exercises.saveExercise(bench.copy(id = 0, name = "Bankdrücken Kurzhantel", equipment = Equipment.DUMBBELL))
        // "Bankdrücken" itself is now ambiguous only if the exact one is gone.
        exercises.setArchived(1, true)
        answer = ParsedLog(listOf(ParsedExercise("Bankdrücken", null, null, listOf(ParsedSetGroup(1, 80.0, null, 8, null, null, SetType.WORKING)))))
        val vm = viewModel()
        vm.setText("Bankdrücken 80 kg 8")
        vm.requestParse()

        val entry = vm.uiState.value.proposal!!.entries.single()
        assertNull(entry.selected)
        vm.save()
        assertNull(vm.uiState.value.saved)
        assertNull(workouts.current)

        vm.select(0, 1)
        vm.save()

        assertNotNull(vm.uiState.value.saved)
        assertEquals(1, workouts.current!!.exercises.size)
    }

    @Test
    fun savedTextIsNotLongerThanTheLimit() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()

        vm.setText("x".repeat(LoggingInput.MAX_TEXT + 50))

        assertEquals(LoggingInput.MAX_TEXT, vm.uiState.value.text.length)
    }
}
