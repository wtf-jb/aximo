package io.github.wtfjb.aximo.domain.plan

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlanBuilderTest {

    private fun exercise(id: Long, name: String, equipment: Equipment = Equipment.BARBELL, type: ExerciseType = ExerciseType.STRENGTH, archived: Boolean = false) =
        Exercise(id = id, name = name, type = type, equipment = equipment, primaryMuscles = setOf(MuscleGroup.CHEST), archived = archived)

    private val input = PlanInput(
        PlanRequest(daysPerWeek = 2),
        listOf(
            PlanExercise(1, "Bankdrücken", Equipment.BARBELL, setOf(BodyRegion.CHEST), 6, 10),
            PlanExercise(2, "Kniebeuge", Equipment.BARBELL, setOf(BodyRegion.LEGS), 5, 8),
        ),
    )

    private fun pe(id: Long, sets: Int = 3, min: Int = 8, max: Int = 10, rir: Int? = 2) = GeneratedPlanExercise(id, sets, min, max, rir)

    @Test
    fun buildsValidPlan() {
        val plan = PlanBuilder.build(
            GeneratedPlan("Zwei Tage.", listOf(GeneratedRoutine(" Push ", listOf(pe(1))), GeneratedRoutine("Legs", listOf(pe(2, rir = null))))),
            input,
        )

        assertEquals("Zwei Tage.", plan.summary)
        assertEquals(listOf("Push", "Legs"), plan.routines.map { it.name })
        assertEquals(ProposedExercise(1, "Bankdrücken", 3, 8, 10, 2), plan.routines[0].exercises.single())
        assertNull(plan.routines[1].exercises.single().targetRir)
        assertEquals(0, plan.dropped)
    }

    @Test
    fun dropsUnknownDuplicateAndOutOfRangeExercises() {
        val plan = PlanBuilder.build(
            GeneratedPlan(
                "x",
                listOf(
                    GeneratedRoutine(
                        "A",
                        listOf(
                            pe(1),
                            pe(1), // duplicate
                            pe(99), // not offered
                            pe(2, sets = 0), // sets
                            pe(2, min = 10, max = 8), // reps
                            pe(2, rir = 6), // RIR
                            pe(2, sets = 11),
                            pe(2, max = 51),
                        ),
                    ),
                ),
                dropped = 1,
            ),
            input,
        )

        assertEquals(listOf(1L), plan.routines.single().exercises.map { it.exerciseId })
        assertEquals(1 + 7, plan.dropped)
    }

    @Test
    fun dropsEmptyAndUnnamedRoutinesAndExtraDays() {
        val plan = PlanBuilder.build(
            GeneratedPlan(
                "x",
                listOf(
                    GeneratedRoutine("Leer", listOf(pe(99))),
                    GeneratedRoutine("  ", listOf(pe(1))),
                    GeneratedRoutine("Gut", listOf(pe(1))),
                    GeneratedRoutine("Auch gut", listOf(pe(2))),
                    GeneratedRoutine("Zu viel", listOf(pe(2))), // only 2 days requested
                ),
            ),
            input,
        )

        assertEquals(listOf("Gut", "Auch gut"), plan.routines.map { it.name })
        // Leer: 1 exercise + 1 routine, blank name: 1 routine, extra day: 1.
        assertEquals(1 + 1 + 1 + 1, plan.dropped)
    }

    @Test
    fun nothingUsableThrows() {
        try {
            PlanBuilder.build(GeneratedPlan("x", listOf(GeneratedRoutine("A", listOf(pe(99))))), input)
            fail("Expected PlanException")
        } catch (e: PlanException) {
            assertEquals(PlanException.Reason.NOTHING_USABLE, e.reason)
        }
    }

    @Test
    fun capsNameLengthAndExerciseCount() {
        val many = PlanInput(input.request, (1L..20L).map { PlanExercise(it, "E$it", Equipment.BARBELL, emptySet(), 6, 10) })
        val plan = PlanBuilder.build(GeneratedPlan("x", listOf(GeneratedRoutine("N".repeat(80), (1L..20L).map { pe(it) }))), many)

        assertEquals(PlanBuilder.MAX_NAME_LENGTH, plan.routines.single().name.length)
        assertEquals(PlanBuilder.MAX_EXERCISES_PER_ROUTINE, plan.routines.single().exercises.size)
    }

    @Test
    fun routineEntriesKeepOrderAndTargets() {
        val routine = ProposedRoutine("A", listOf(ProposedExercise(2, "K", 4, 5, 8, 1), ProposedExercise(1, "B", 3, 8, 10, null)))

        val entries = routine.toEntries()

        assertEquals(listOf(2L, 1L), entries.map { it.exerciseId })
        assertEquals(listOf(0, 1), entries.map { it.position })
        assertEquals(4, entries[0].targetSets)
        assertNull(entries[1].targetRir)
    }

    @Test
    fun withoutRemovesOneRoutine() {
        val proposal = PlanProposal("x", listOf(ProposedRoutine("A", emptyList()), ProposedRoutine("B", emptyList())))

        assertEquals(listOf("B"), proposal.without(0).routines.map { it.name })
        assertEquals(proposal, proposal.without(5))
    }

    @Test
    fun inputKeepsOnlyUsableExercisesSortedByName() {
        val all = listOf(
            exercise(1, "Rudern", Equipment.CABLE),
            exercise(2, "bankdrücken"),
            exercise(3, "Laufen", type = ExerciseType.CARDIO),
            exercise(4, "Alt", archived = true),
            exercise(5, "Kurzhantelcurl", Equipment.DUMBBELL),
            exercise(6, "Eigene Übung", Equipment.OTHER),
        )

        val barbellAndOther = PlanInput.from(PlanRequest(equipment = setOf(Equipment.BARBELL)), all)

        assertEquals(listOf("bankdrücken", "Eigene Übung"), barbellAndOther.exercises.map { it.name })
        assertTrue(PlanInput.from(PlanRequest(), all).exercises.none { it.exerciseId == 3L || it.exerciseId == 4L })
    }

    @Test
    fun requestDerivesSessionSizeAndCleansRestrictions() {
        assertEquals(3, PlanRequest(minutes = 30).exercisesPerRoutine)
        assertEquals(6, PlanRequest(minutes = 60).exercisesPerRoutine)
        assertEquals(9, PlanRequest(minutes = 90).exercisesPerRoutine)
        assertEquals("Knie", PlanRequest(restrictions = "  Knie ").cleanRestrictions)
        assertEquals(PlanRequest.MAX_RESTRICTIONS, PlanRequest(restrictions = "x".repeat(500)).cleanRestrictions.length)
    }
}
