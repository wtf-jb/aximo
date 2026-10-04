package io.github.wtfjb.aximo.ui.plangen

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.plan.GeneratedPlan
import io.github.wtfjb.aximo.domain.plan.GeneratedPlanExercise
import io.github.wtfjb.aximo.domain.plan.GeneratedRoutine
import io.github.wtfjb.aximo.domain.plan.PlanException
import io.github.wtfjb.aximo.domain.plan.PlanGoal
import io.github.wtfjb.aximo.domain.plan.PlanInput
import io.github.wtfjb.aximo.domain.plan.PlanOptions
import io.github.wtfjb.aximo.domain.plan.PlanRequest
import io.github.wtfjb.aximo.domain.plan.PlanService
import io.github.wtfjb.aximo.ui.ai.FakeAiPreferences
import io.github.wtfjb.aximo.ui.ai.FakeAiProfileRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanGeneratorViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val exercises = FakeExerciseRepository(
        (1L..6L).map { Exercise(id = it, name = "Übung $it", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL) },
    )
    private val routines = FakeRoutineRepository()
    private val profiles = FakeAiProfileRepository()
    private val preferences = FakeAiPreferences()

    private var calls = 0
    private var failure: Exception? = null
    private var sent: PlanInput? = null

    private val service = PlanService(
        exercises = exercises,
        routines = routines,
        profiles = profiles,
        factory = { _, _ ->
            object : AiProvider {
                override suspend fun complete(request: AiRequest): String = error("not used, the generator is faked")
            }
        },
        generator = { _, input, _ ->
            calls++
            sent = input
            failure?.let { throw it }
            GeneratedPlan(
                "Zwei Tage.",
                listOf(
                    GeneratedRoutine("A", listOf(GeneratedPlanExercise(1, 3, 6, 8, 2))),
                    GeneratedRoutine("B", listOf(GeneratedPlanExercise(2, 3, 8, 12, null))),
                ),
            )
        },
    )

    private fun viewModel() = PlanGeneratorViewModel(service, preferences, { "de" }, { "data: ${it.exercises.size}" })

    @Test
    fun formChangesUpdateTheRequest() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()

        vm.setGoal(PlanGoal.STRENGTH)
        vm.setDays(5)
        vm.setMinutes(90)
        vm.setRestrictions("x".repeat(400))
        vm.toggleEquipment(Equipment.CABLE)

        val request = vm.uiState.value.request
        assertEquals(PlanGoal.STRENGTH, request.goal)
        assertEquals(5, request.daysPerWeek)
        assertEquals(90, request.minutes)
        assertEquals(PlanRequest.MAX_RESTRICTIONS, request.restrictions.length)
        assertFalse(Equipment.CABLE in request.equipment)
    }

    @Test
    fun lastEquipmentStaysOn() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()
        PlanOptions.equipment.filter { it != Equipment.BARBELL }.forEach(vm::toggleEquipment)

        vm.toggleEquipment(Equipment.BARBELL)

        assertEquals(setOf(Equipment.BARBELL), vm.uiState.value.request.equipment)
    }

    @Test
    fun firstRequestAsksForConsent() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        val vm = viewModel()

        vm.requestPlan()
        assertTrue(vm.uiState.value.showNotice)
        assertEquals(0, calls)

        vm.acceptNotice()

        assertTrue(preferences.accepted.value)
        assertFalse(vm.uiState.value.showNotice)
        assertEquals(1, calls)
        assertEquals(listOf("A", "B"), vm.uiState.value.proposal!!.routines.map { it.name })
    }

    @Test
    fun afterConsentGoesStraightToTheAi() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        preferences.accepted.value = true
        val vm = viewModel()

        vm.requestPlan()

        assertFalse(vm.uiState.value.showNotice)
        assertNotNull(vm.uiState.value.proposal)
        assertFalse(vm.uiState.value.running)
        // Drafting alone does not save anything.
        assertTrue(routines.all.isEmpty())
    }

    @Test
    fun sentDataShowsTheInput() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()

        vm.showSentData()
        assertEquals("data: 6", vm.uiState.value.sentData)

        vm.dismissSentData()
        assertNull(vm.uiState.value.sentData)
    }

    @Test
    fun errorsAreShownAndTheFormStays() = runTest(UnconfinedTestDispatcher()) {
        preferences.accepted.value = true
        val vm = viewModel()

        vm.requestPlan()
        assertEquals(PlanError.Plan(PlanException.Reason.NO_PROFILE), vm.uiState.value.error)

        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        failure = AiException(AiException.Reason.TIMEOUT)
        vm.requestPlan()
        assertEquals(PlanError.Ai(AiException.Reason.TIMEOUT, null, null), vm.uiState.value.error)
        assertNull(vm.uiState.value.proposal)

        failure = null
        vm.requestPlan()
        assertNull(vm.uiState.value.error)
        assertNotNull(vm.uiState.value.proposal)
    }

    @Test
    fun saveStoresTheRemainingRoutinesAndCloses() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        preferences.accepted.value = true
        val vm = viewModel()
        vm.requestPlan()

        vm.removeRoutine(0)
        vm.save()

        assertEquals(listOf("B"), routines.all.map { it.routine.name })
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun discardGoesBackToTheFormWithTheSameWishes() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        preferences.accepted.value = true
        val vm = viewModel()
        vm.setDays(4)
        vm.requestPlan()

        vm.discard()

        assertNull(vm.uiState.value.proposal)
        assertEquals(4, vm.uiState.value.request.daysPerWeek)
        assertTrue(routines.all.isEmpty())
    }

    @Test
    fun saveWithoutRoutinesDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        preferences.accepted.value = true
        val vm = viewModel()
        vm.requestPlan()
        vm.removeRoutine(0)
        vm.removeRoutine(0)

        vm.save()

        assertFalse(vm.uiState.value.done)
        assertTrue(routines.all.isEmpty())
    }
}
