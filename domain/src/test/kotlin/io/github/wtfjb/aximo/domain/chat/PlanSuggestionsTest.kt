package io.github.wtfjb.aximo.domain.chat

import io.github.wtfjb.aximo.domain.StatsTestData.bench
import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.review.PlanEntry
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.PlanRoutine
import io.github.wtfjb.aximo.domain.review.SuggestionApplier
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanSuggestionsTest {

    private val crossover = CatalogEntry(
        id = "Cable_Crossover",
        name = "Cable Crossover",
        type = ExerciseType.STRENGTH,
        equipment = Equipment.CABLE,
        primary = setOf(MuscleGroup.CHEST),
        secondary = emptySet(),
        repMin = 10,
        repMax = 15,
        instructions = listOf("Pull."),
    )
    private val catalog = listOf(crossover)

    private fun new(name: String, catalogName: String? = null) =
        PlanExerciseRef.New(name, ExerciseType.STRENGTH, Equipment.OTHER, catalogName = catalogName)

    private fun entry(ref: PlanExerciseRef, sets: Int = 3) = PlanEntry(ref, sets, 8, 12, 2)

    private fun plan(vararg routines: PlanRoutine) = SuggestionChange.CreatePlan(routines.toList())

    @Test
    fun newExerciseIsMatchedToTheLibrary() {
        val result = PlanSuggestions.resolve(
            plan(PlanRoutine("  Ganzkörper A ", listOf(entry(new("kabelzug-fliegende", catalogName = "Cable Crossover"))))),
            listOf(bench),
            catalog,
        )

        val routine = result.plan!!.routines.single()
        assertEquals("Ganzkörper A", routine.name)
        val ref = routine.exercises.single().exercise as PlanExerciseRef.New
        assertEquals("Kabelzug-fliegende", ref.name)
        assertEquals(crossover.catalogId, ref.catalogId)
        assertEquals(Equipment.CABLE, ref.equipment)
        assertEquals(0, result.dropped)
    }

    @Test
    fun ownExerciseWinsByNameOrLibraryEntry() {
        val ownCrossover = bench.copy(id = 9, name = "Cable Crossover (eigene)", catalogId = crossover.catalogId)
        val result = PlanSuggestions.resolve(
            plan(PlanRoutine("A", listOf(entry(new("bankdrücken")), entry(new("Fliegende", catalogName = "Cable Crossover"))))),
            listOf(bench, ownCrossover),
            catalog,
        )

        assertEquals(
            listOf(PlanExerciseRef.Existing(bench.id), PlanExerciseRef.Existing(ownCrossover.id)),
            result.plan!!.routines.single().exercises.map { it.exercise },
        )
    }

    @Test
    fun unknownNameStaysNewWithoutLibraryEntry() {
        val result = PlanSuggestions.resolve(plan(PlanRoutine("A", listOf(entry(new("Landmine Press"))))), emptyList(), catalog)

        val ref = result.plan!!.routines.single().exercises.single().exercise as PlanExerciseRef.New
        assertNull(ref.catalogId)
        assertEquals(Equipment.OTHER, ref.equipment)
    }

    @Test
    fun invalidEntriesAndRoutinesAreDroppedAndCounted() {
        val result = PlanSuggestions.resolve(
            plan(
                PlanRoutine("A", listOf(entry(PlanExerciseRef.Existing(bench.id)), entry(PlanExerciseRef.Existing(bench.id)), entry(PlanExerciseRef.Existing(99)), entry(new("x"), sets = 0))),
                PlanRoutine("B", listOf(entry(PlanExerciseRef.Existing(99)))),
                PlanRoutine(" ", listOf(entry(PlanExerciseRef.Existing(bench.id)))),
            ),
            listOf(bench),
            catalog,
        )

        assertEquals(listOf("A"), result.plan!!.routines.map { it.name })
        assertEquals(1, result.plan!!.routines.single().exercises.size)
        // duplicate, unknown id, sets 0 in A; unknown id + empty B; blank name.
        assertEquals(6, result.dropped)
    }

    @Test
    fun nothingLeftGivesNoPlan() {
        val result = PlanSuggestions.resolve(plan(PlanRoutine("A", listOf(entry(PlanExerciseRef.Existing(99))))), emptyList(), catalog)

        assertNull(result.plan)
        assertEquals(2, result.dropped)
    }

    @Test
    fun keepsAtMostSevenRoutines() {
        val routines = List(9) { PlanRoutine("R$it", listOf(entry(PlanExerciseRef.Existing(bench.id)))) }

        val result = PlanSuggestions.resolve(SuggestionChange.CreatePlan(routines), listOf(bench), catalog)

        assertEquals(SuggestionApplier.MAX_PLAN_ROUTINES, result.plan!!.routines.size)
        assertEquals(2, result.dropped)
    }

    @Test
    fun newExerciseTakesLibraryDataAndKeepsItsName() {
        val ref = PlanExerciseRef.New("Kabelzug-Fliegende", ExerciseType.STRENGTH, Equipment.CABLE, catalogId = crossover.catalogId)

        val exercise = PlanSuggestions.newExercise(ref, catalog, TrainingSettings())

        assertEquals("Kabelzug-Fliegende", exercise.name)
        assertEquals(crossover.catalogId, exercise.catalogId)
        assertEquals(setOf(MuscleGroup.CHEST), exercise.primaryMuscles)
        assertEquals(10, exercise.repRangeMin)
    }

    @Test
    fun selectedLeavesOutSwitchedOffRoutines() {
        val p = plan(PlanRoutine("A", emptyList()), PlanRoutine("B", emptyList()), PlanRoutine("C", emptyList()))

        assertEquals(listOf("A", "C"), PlanSuggestions.selected(p, setOf(1)).map { it.name })
    }
}
