package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows

class TrainingSettingsTest {

    @Test
    fun defaultsAreKgTwoMinutesAndStandardSteps() {
        val settings = TrainingSettings()
        assertEquals(WeightUnit.KG, settings.unit)
        assertEquals(120, settings.restSeconds)
        assertEquals(WeightSteps(2.5, 2.0), settings.steps)
    }

    @Test
    fun dumbbellsUseTheirOwnStep() {
        val steps = WeightSteps.standard(WeightUnit.KG)
        assertEquals(2.0, steps.forEquipment(Equipment.DUMBBELL), 0.0)
        assertEquals(2.5, steps.forEquipment(Equipment.BARBELL), 0.0)
        assertEquals(2.5, steps.forEquipment(Equipment.MACHINE), 0.0)
    }

    @Test
    fun standardLbsStepsAreFivePounds() {
        val steps = WeightSteps.standard(WeightUnit.LBS)
        assertEquals(5.0, WeightUnit.LBS.fromKg(steps.barbellKg), 1e-9)
        assertEquals(5.0, WeightUnit.LBS.fromKg(steps.dumbbellKg), 1e-9)
    }

    @Test
    fun switchingUnitMovesStandardStepsAlong() {
        val lbs = TrainingSettings().withUnit(WeightUnit.LBS)
        assertEquals(WeightUnit.LBS, lbs.unit)
        assertEquals(WeightSteps.standard(WeightUnit.LBS), lbs.steps)
        assertEquals(WeightSteps.standard(WeightUnit.KG), lbs.withUnit(WeightUnit.KG).steps)
    }

    @Test
    fun switchingUnitKeepsCustomSteps() {
        val custom = TrainingSettings(steps = WeightSteps(1.25, 1.0))
        assertEquals(WeightSteps(1.25, 1.0), custom.withUnit(WeightUnit.LBS).steps)
    }

    @Test
    fun switchingToTheSameUnitChangesNothing() {
        val settings = TrainingSettings(restSeconds = 90)
        assertEquals(settings, settings.withUnit(WeightUnit.KG))
    }

    @Test
    fun invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { WeightSteps(0.0, 2.0) }
        assertThrows(IllegalArgumentException::class.java) { TrainingSettings(restSeconds = -1) }
    }

    @Test
    fun newDraftUsesTheSettings() {
        val draft = ExerciseDraft.new(TrainingSettings(restSeconds = 90))
        assertEquals("2.5", draft.increment)
        assertEquals("90", draft.restSeconds)
        assertEquals(WeightUnit.KG, draft.unit)

        val lbs = ExerciseDraft.new(TrainingSettings().withUnit(WeightUnit.LBS))
        assertEquals("5", lbs.increment)
        assertEquals(WeightUnit.LBS, lbs.unit)
    }

    @Test
    fun defaultIncrementFollowsTheEquipment() {
        val steps = WeightSteps.standard(WeightUnit.KG)
        val draft = ExerciseDraft.new(TrainingSettings())
        val dumbbell = draft.withEquipment(Equipment.DUMBBELL, steps)
        assertEquals("2", dumbbell.increment)
        assertEquals(Equipment.DUMBBELL, dumbbell.equipment)
        assertEquals("2.5", dumbbell.withEquipment(Equipment.CABLE, steps).increment)
    }

    @Test
    fun customIncrementStaysWhenTheEquipmentChanges() {
        val steps = WeightSteps.standard(WeightUnit.KG)
        val draft = ExerciseDraft.new(TrainingSettings()).copy(increment = "1,25")
        assertEquals("1,25", draft.withEquipment(Equipment.DUMBBELL, steps).increment)
    }
}
