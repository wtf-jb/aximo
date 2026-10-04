package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.plan.GeneratedPlanExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PlanParserTest {

    @Test
    fun parsesPlan() {
        val plan = PlanParser.parse(
            """
            ```json
            {"summary": "Drei Tage Ganzkörper.", "routines": [
              {"name": "Ganzkörper A", "exercises": [
                {"exercise_id": 1, "sets": 3, "rep_min": 6, "rep_max": 8, "target_rir": 2},
                {"exercise_id": 2, "sets": 3, "rep_min": 8, "rep_max": 12, "target_rir": null},
                {"exercise_id": 3, "sets": 2, "rep_min": 10, "rep_max": 15}
              ]}
            ]}
            ```
            """.trimIndent(),
        )

        assertEquals("Drei Tage Ganzkörper.", plan.summary)
        assertEquals(0, plan.dropped)
        val routine = plan.routines.single()
        assertEquals("Ganzkörper A", routine.name)
        assertEquals(
            listOf(
                GeneratedPlanExercise(1, 3, 6, 8, 2),
                GeneratedPlanExercise(2, 3, 8, 12, null),
                GeneratedPlanExercise(3, 2, 10, 15, null),
            ),
            routine.exercises,
        )
    }

    @Test
    fun dropsMalformedExercisesAndRoutines() {
        val plan = PlanParser.parse(
            """
            {"summary": "s", "routines": [
              {"name": "A", "exercises": [
                {"exercise_id": 1, "sets": 3, "rep_min": 6, "rep_max": 8},
                {"exercise_id": "x", "sets": 3, "rep_min": 6, "rep_max": 8},
                {"exercise_id": 2, "sets": 3, "rep_min": 6},
                {"exercise_id": 3, "sets": 3, "rep_min": 6, "rep_max": 8, "target_rir": "hoch"},
                "kaputt"
              ]},
              {"name": "", "exercises": []},
              {"name": "C"},
              42
            ]}
            """.trimIndent(),
        )

        assertEquals(listOf(1L), plan.routines.single().exercises.map { it.exerciseId })
        assertEquals(4 + 3, plan.dropped)
    }

    @Test
    fun invalidAnswersThrow() {
        listOf(
            "Ich kann leider nicht helfen.",
            """{"routines": []}""",
            """{"summary": "  ", "routines": []}""",
            """{"summary": "s"}""",
            """{"summary": "s", "routines": "x"}""",
        ).forEach { text ->
            try {
                PlanParser.parse(text)
                fail("Expected AiException for: $text")
            } catch (e: AiException) {
                assertEquals(AiException.Reason.INVALID_RESPONSE, e.reason)
            }
        }
    }

    @Test
    fun emptyRoutineListParsesButIsEmpty() {
        assertEquals(emptyList<Any>(), PlanParser.parse("""{"summary": "s", "routines": []}""").routines)
    }
}
