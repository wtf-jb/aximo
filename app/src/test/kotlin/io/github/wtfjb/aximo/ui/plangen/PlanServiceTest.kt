package io.github.wtfjb.aximo.ui.plangen

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.plan.GeneratedPlan
import io.github.wtfjb.aximo.domain.plan.GeneratedPlanExercise
import io.github.wtfjb.aximo.domain.plan.GeneratedRoutine
import io.github.wtfjb.aximo.domain.plan.PlanException
import io.github.wtfjb.aximo.domain.plan.PlanInput
import io.github.wtfjb.aximo.domain.plan.PlanRequest
import io.github.wtfjb.aximo.domain.plan.PlanService
import io.github.wtfjb.aximo.ui.ai.FakeAiProfileRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class PlanServiceTest {

    private val exercises = FakeExerciseRepository(
        (1L..PlanInput.MIN_EXERCISES.toLong()).map {
            Exercise(id = it, name = "Übung $it", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
        } + Exercise(id = 50, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER),
    )
    private val routines = FakeRoutineRepository(listOf(RoutineWithExercises(Routine(id = 1, name = "Alt", position = 4), emptyList())))
    private val profiles = FakeAiProfileRepository()

    private var answer = GeneratedPlan(
        "Zwei Tage.",
        listOf(
            GeneratedRoutine("Tag A", listOf(GeneratedPlanExercise(1, 3, 6, 8, 2), GeneratedPlanExercise(2, 3, 8, 10, null))),
            GeneratedRoutine("Tag B", listOf(GeneratedPlanExercise(3, 4, 5, 5, 1))),
        ),
    )
    private var sentInput: PlanInput? = null
    private var sentLanguage: String? = null
    private var usedKey: String? = null

    private val service = PlanService(
        exercises = exercises,
        routines = routines,
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
    )

    private suspend fun expectPlanError(request: PlanRequest = PlanRequest(daysPerWeek = 2)): PlanException.Reason {
        try {
            service.generate(request, "de")
        } catch (e: PlanException) {
            return e.reason
        }
        fail("Expected PlanException")
        throw AssertionError()
    }

    @Test
    fun needsProfileAndEnoughExercises() = runTest {
        assertEquals(PlanException.Reason.NO_PROFILE, expectPlanError())

        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        // Only dumbbells chosen, but all exercises use a barbell.
        assertEquals(PlanException.Reason.NOT_ENOUGH_EXERCISES, expectPlanError(PlanRequest(daysPerWeek = 2, equipment = setOf(Equipment.DUMBBELL))))
    }

    @Test
    fun generatesDraftWithoutSavingAnything() = runTest {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Set("geheim"))

        val proposal = service.generate(PlanRequest(daysPerWeek = 2), "de")

        assertEquals(listOf("Tag A", "Tag B"), proposal.routines.map { it.name })
        assertEquals("Übung 1", proposal.routines[0].exercises[0].name)
        assertEquals("geheim", usedKey)
        assertEquals("de", sentLanguage)
        // No cardio exercise is offered to the AI.
        assertEquals(PlanInput.MIN_EXERCISES, sentInput!!.exercises.size)
        assertEquals(1, routines.all.size)
    }

    @Test
    fun savesRoutinesAtTheEndOfTheList() = runTest {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        val proposal = service.generate(PlanRequest(daysPerWeek = 2), "en").without(1)

        val ids = service.save(proposal)

        assertEquals(1, ids.size)
        val saved = routines.getRoutine(ids.single())!!
        assertEquals("Tag A", saved.routine.name)
        assertEquals(5, saved.routine.position)
        assertEquals(listOf(1L, 2L), saved.exercises.map { it.exerciseId })
        assertEquals(listOf(0, 1), saved.exercises.map { it.position })
        assertNull(saved.exercises[1].targetRir)
        assertEquals(2, routines.all.size)
    }
}
