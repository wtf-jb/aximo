package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.cardio.CardioActivities
import io.github.wtfjb.aximo.domain.cardio.DefaultActivity
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardioActivitiesTest {

    private val defaults = CardioActivities.defaults { it.name }.mapIndexed { i, e -> e.copy(id = i + 1L) }
    private val bench = Exercise(id = 10, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val swim = Exercise(id = 11, name = "Schwimmen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)
    private val elliptical = Exercise(id = 12, name = "Crosstrainer", type = ExerciseType.CARDIO, equipment = Equipment.MACHINE)

    private fun entry(exerciseId: Long) =
        CardioEntry(exerciseId = exerciseId, startedAt = Instant.fromEpochSeconds(1_790_000_000), durationSec = 600)

    @Test
    fun `defaults are four cardio exercises with catalog ids`() {
        assertEquals(DefaultActivity.entries.map { it.catalogId }, defaults.map { it.catalogId })
        assertTrue(defaults.all { it.type == ExerciseType.CARDIO })
    }

    @Test
    fun `defaults are needed only without any cardio exercise`() {
        assertTrue(CardioActivities.needsDefaults(emptyList()))
        assertTrue(CardioActivities.needsDefaults(listOf(bench)))
        assertFalse(CardioActivities.needsDefaults(listOf(bench, swim)))
        assertFalse(CardioActivities.needsDefaults(listOf(swim.copy(archived = true))))
    }

    @Test
    fun `choices list defaults first, then own activities by name`() {
        val choices = CardioActivities.choices(listOf(swim, bench, elliptical) + defaults.reversed())
        assertEquals(listOf("RUNNING", "CYCLING", "ROWING", "OTHER", "Crosstrainer", "Schwimmen"), choices.map { it.name })
    }

    @Test
    fun `archived activities are hidden unless kept`() {
        val archived = swim.copy(archived = true)
        assertEquals(emptyList<Exercise>(), CardioActivities.choices(listOf(archived)))
        assertEquals(listOf(archived), CardioActivities.choices(listOf(archived), keepId = archived.id))
    }

    @Test
    fun `pace style follows the default activity`() {
        assertEquals(PaceStyle.PER_KM, CardioActivities.paceStyle(defaults[0]))
        assertEquals(PaceStyle.SPEED, CardioActivities.paceStyle(defaults[1]))
        assertEquals(PaceStyle.PER_500M, CardioActivities.paceStyle(defaults[2]))
        assertEquals(PaceStyle.SPEED, CardioActivities.paceStyle(swim))
    }

    @Test
    fun `preselect the last used activity, else the first`() {
        val choices = CardioActivities.choices(defaults + swim)
        assertEquals(swim.id, CardioActivities.preselect(choices, entry(swim.id)))
        assertEquals(defaults[0].id, CardioActivities.preselect(choices, null))
        assertEquals(defaults[0].id, CardioActivities.preselect(choices, entry(99)))
        assertNull(CardioActivities.preselect(emptyList(), null))
    }
}
