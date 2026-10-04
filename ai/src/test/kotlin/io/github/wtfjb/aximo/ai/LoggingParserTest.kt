package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.logging.ParsedExercise
import io.github.wtfjb.aximo.domain.logging.ParsedSetGroup
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.units.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class LoggingParserTest {

    @Test
    fun parsesExercisesAndSets() {
        val log = LoggingParser.parse(
            """
            ```json
            {"exercises": [
              {"name": "Bankdrücken", "type": null, "equipment": null, "sets": [
                {"count": 3, "weight": 80, "weight_unit": "kg", "reps": 8, "rir": 2, "rpe": null, "set_type": "working"}
              ]},
              {"name": "Kniebeuge", "type": "strength", "equipment": "Barbell", "sets": [
                {"count": 1, "weight": 100.5, "weight_unit": "lbs", "reps": 5, "rir": null, "rpe": 8.5, "set_type": "warm_up"},
                {"weight": null, "reps": 3}
              ]}
            ]}
            ```
            """.trimIndent(),
        )

        assertEquals(0, log.dropped)
        assertEquals(
            listOf(
                ParsedExercise(
                    "Bankdrücken", null, null,
                    listOf(ParsedSetGroup(3, 80.0, WeightUnit.KG, 8, 2, null, SetType.WORKING)),
                ),
                ParsedExercise(
                    "Kniebeuge", ExerciseType.STRENGTH, Equipment.BARBELL,
                    listOf(
                        ParsedSetGroup(1, 100.5, WeightUnit.LBS, 5, null, 8.5, SetType.WARM_UP),
                        ParsedSetGroup(1, null, null, 3, null, null, SetType.WORKING),
                    ),
                ),
            ),
            log.exercises,
        )
    }

    @Test
    fun emptyExerciseListIsValid() {
        val log = LoggingParser.parse("""{"exercises": []}""")

        assertEquals(emptyList<ParsedExercise>(), log.exercises)
        assertEquals(0, log.dropped)
    }

    @Test
    fun unknownHintsCountAsNotSaid() {
        val log = LoggingParser.parse("""{"exercises": [{"name": "X", "type": "cardio", "equipment": "sofa", "sets": [{"reps": 1}]}]}""")

        assertNull(log.exercises.single().type)
        assertNull(log.exercises.single().equipment)
    }

    @Test
    fun acceptsLbAndWarmupSpellings() {
        val set = LoggingParser.parse("""{"exercises": [{"name": "X", "sets": [{"reps": 1, "weight_unit": "LB", "set_type": "warmup"}]}]}""")
            .exercises.single().sets.single()

        assertEquals(WeightUnit.LBS, set.weightUnit)
        assertEquals(SetType.WARM_UP, set.setType)
    }

    @Test
    fun dropsMalformedSetsAndExercises() {
        val log = LoggingParser.parse(
            """
            {"exercises": [
              {"name": "A", "sets": [
                {"reps": 8},
                {"reps": "acht"},
                {"weight": 80},
                {"reps": 8, "weight": "viel"},
                {"reps": 8, "count": "drei"},
                {"reps": 8, "weight_unit": "stone"},
                {"reps": 8, "set_type": "super"},
                {"reps": 8, "rir": "zwei"},
                {"reps": 8, "rpe": "hoch"},
                "x"
              ]},
              {"name": "", "sets": [{"reps": 8}]},
              {"name": "B"},
              {"sets": []},
              "kaputt"
            ]}
            """.trimIndent(),
        )

        assertEquals(1, log.exercises.size)
        assertEquals(1, log.exercises.single().sets.size)
        assertEquals(9 + 4, log.dropped)
    }

    @Test
    fun invalidAnswersThrow() {
        for (text in listOf("", "kein json", "[]", """{"summary": "x"}""", """{"exercises": "x"}""")) {
            try {
                LoggingParser.parse(text)
                fail(text)
            } catch (e: AiException) {
                assertEquals(AiException.Reason.INVALID_RESPONSE, e.reason)
            }
        }
    }
}
