package io.github.wtfjb.aximo.data.catalog

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogFileTest {

    @Test
    fun parsesEntries() {
        val entries = CatalogFile.parse(
            """
            [{"id":"Barbell_Curl","name":"Barbell Curl","type":"STRENGTH","equipment":"BARBELL","primary":["BICEPS"],
              "secondary":["FOREARMS"],"repMin":8,"repMax":12,"instructions":["Stand up.","Curl.","Lower."],"extra":1}]
            """.trimIndent(),
        )

        val entry = entries.single()
        assertEquals("Barbell_Curl", entry.id)
        assertEquals(ExerciseType.STRENGTH, entry.type)
        assertEquals(Equipment.BARBELL, entry.equipment)
        assertEquals(setOf(MuscleGroup.BICEPS), entry.primary)
        assertEquals(setOf(MuscleGroup.FOREARMS), entry.secondary)
        assertEquals(8..12, entry.repMin..entry.repMax)
        assertEquals(3, entry.instructions.size)
    }

    @Test
    fun skipsRowsThatDoNotFitTheModel() {
        val entries = CatalogFile.parse(
            """
            [{"id":"ok","name":"Ok","type":"BODYWEIGHT","equipment":"BODYWEIGHT","primary":["ABS"],"repMin":8,"repMax":15},
             {"id":"unknown_muscle","name":"X","type":"STRENGTH","equipment":"BARBELL","primary":["NECK"],"repMin":8,"repMax":12},
             {"id":"cardio","name":"X","type":"CARDIO","equipment":"OTHER","primary":["QUADS"],"repMin":8,"repMax":12},
             {"id":"no_primary","name":"X","type":"STRENGTH","equipment":"BARBELL","primary":[],"repMin":8,"repMax":12},
             {"id":"bad_reps","name":"X","type":"STRENGTH","equipment":"BARBELL","primary":["ABS"],"repMin":12,"repMax":8}]
            """.trimIndent(),
        )

        assertEquals(listOf("ok"), entries.map { it.id })
    }
}
