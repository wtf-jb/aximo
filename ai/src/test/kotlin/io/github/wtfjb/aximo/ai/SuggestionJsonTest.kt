package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.review.PlanEntry
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.PlanRoutine
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionJsonTest {

    private fun parse(json: String) = SuggestionJson.parse(Json.parseToJsonElement(json).jsonObject)

    @Test
    fun routineChanges() {
        assertEquals(
            SuggestionChange.ReplaceExercise(7, 1, 4),
            parse("""{"type":"replace_exercise","routine_id":7,"exercise_id":1,"new_exercise_id":4,"rationale":"r"}""")!!.change,
        )
        // Wire positions are 1-based.
        assertEquals(
            SuggestionChange.MoveExercise(7, 1, from = 2, to = 0),
            parse("""{"type":"move_exercise","routine_id":7,"exercise_id":1,"from":3,"to":1,"rationale":"r"}""")!!.change,
        )
        assertEquals(
            SuggestionChange.RenameRoutine(7, "Push A", "Oberkörper A"),
            parse("""{"type":"rename_routine","routine_id":7,"from":"Push A","to":"Oberkörper A","rationale":"r"}""")!!.change,
        )
        assertEquals(
            SuggestionChange.DeleteRoutine(7, "Push A"),
            parse("""{"type":"delete_routine","routine_id":7,"name":"Push A","rationale":"r"}""")!!.change,
        )
        assertNull(parse("""{"type":"rename_routine","routine_id":7,"from":"Push A","rationale":"r"}"""))
        assertNull(parse("""{"type":"replace_exercise","routine_id":7,"exercise_id":1,"rationale":"r"}"""))
    }

    @Test
    fun createPlanWithExistingAndNewExercises() {
        val suggestion = parse(
            """
            {"type":"create_plan","rationale":"3 Tage","routines":[
              {"name":"Ganzkörper A","exercises":[
                {"exercise_id":1,"sets":3,"rep_min":8,"rep_max":12,"target_rir":2},
                {"new_exercise":{"name":"Kabelzug-Fliegende","catalog_name":"Cable Crossover","type":"strength","equipment":"cable"},
                 "sets":3,"rep_min":10,"rep_max":15},
                {"sets":3,"rep_min":8,"rep_max":12},
                {"new_exercise":{"catalog_name":"No name"},"sets":3,"rep_min":8,"rep_max":12}
              ]},
              {"name":"Ganzkörper B","exercises":[]}
            ]}
            """.trimIndent(),
        )!!

        val plan = suggestion.change as SuggestionChange.CreatePlan
        assertEquals("3 Tage", suggestion.rationale)
        assertEquals(listOf("Ganzkörper A", "Ganzkörper B"), plan.routines.map { it.name })
        val (existing, new) = plan.routines[0].exercises
        assertEquals(PlanEntry(PlanExerciseRef.Existing(1), 3, 8, 12, 2), existing)
        assertEquals(PlanExerciseRef.New("Kabelzug-Fliegende", ExerciseType.STRENGTH, Equipment.CABLE, catalogName = "Cable Crossover"), new.exercise)
        assertNull(new.targetRir)
        assertEquals("broken entries are left out", 2, plan.routines[0].exercises.size)
        assertTrue(plan.routines[1].exercises.isEmpty())

        assertNull(parse("""{"type":"create_plan","rationale":"r"}"""))
        assertNull(parse("""{"type":"create_plan","rationale":"r","routines":[]}"""))
    }

    @Test
    fun everyChangeSurvivesEncodeAndParse() {
        val changes = listOf(
            SuggestionChange.SetCount(7, 1, 3, 4),
            SuggestionChange.RepRange(7, 1, 6, 8, 8, 10),
            SuggestionChange.TargetRir(7, 1, null, 2),
            SuggestionChange.AddExercise(7, 2, 3, 8, 12, null),
            SuggestionChange.RemoveExercise(7, 2),
            SuggestionChange.ReplaceExercise(7, 1, 4),
            SuggestionChange.MoveExercise(7, 1, 0, 2),
            SuggestionChange.RenameRoutine(7, "A", "B"),
            SuggestionChange.DeleteRoutine(7, "A"),
            SuggestionChange.CreatePlan(
                listOf(
                    PlanRoutine(
                        "A",
                        listOf(
                            PlanEntry(PlanExerciseRef.Existing(1), 3, 8, 12, 2),
                            PlanEntry(PlanExerciseRef.New("Neu", ExerciseType.BODYWEIGHT, Equipment.BODYWEIGHT, catalogName = "Pushups"), 3, 10, 20, null),
                        ),
                    ),
                ),
            ),
        )

        changes.forEach { change ->
            val back = SuggestionJson.parse(SuggestionJson.encode(change, "r"))!!
            assertEquals(change, back.change)
        }
    }

    @Test
    fun chatPromptListsAllTypes() {
        val system = ChatPrompt.system("de")
        listOf("replace_exercise", "move_exercise", "rename_routine", "delete_routine", "create_plan", "new_exercise", "catalog_name")
            .forEach { assertTrue(it, system.contains(it)) }
        assertTrue("schema is indented inside the array", system.contains("\n    {\"type\": \"set_count\""))
    }
}
