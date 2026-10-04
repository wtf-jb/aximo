package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.exercise.ExerciseFieldError
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.units.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseDraftTest {

    @Test
    fun newDraftOnlyNeedsAName() {
        assertEquals(setOf(ExerciseFieldError.NAME_MISSING), ExerciseDraft().validate())
        assertEquals(emptySet<ExerciseFieldError>(), ExerciseDraft(name = "Bankdrücken").validate())
    }

    @Test
    fun invalidNumbersAreReported() {
        val draft = ExerciseDraft(name = "X", repRangeMin = "12", repRangeMax = "8", increment = "-1", restSeconds = "abc")
        assertEquals(
            setOf(ExerciseFieldError.REP_RANGE_INVALID, ExerciseFieldError.INCREMENT_INVALID, ExerciseFieldError.REST_INVALID),
            draft.validate(),
        )
        assertNull(draft.toExercise())
    }

    @Test
    fun commaIsAcceptedAsDecimalSeparator() {
        val exercise = ExerciseDraft(name = "Seitheben", increment = "1,25").toExercise()!!
        assertEquals(1.25, exercise.incrementKg, 0.0)
    }

    @Test
    fun incrementInLbsIsStoredInKg() {
        val exercise = ExerciseDraft(name = "Curl", increment = "5", unit = WeightUnit.LBS).toExercise()!!
        assertEquals(2.26796185, exercise.incrementKg, 1e-9)
    }

    @Test
    fun nameAndNoteAreTrimmed() {
        val exercise = ExerciseDraft(name = "  Dips ", note = " langsam ").toExercise()!!
        assertEquals("Dips", exercise.name)
        assertEquals("langsam", exercise.note)
    }

    @Test
    fun aMuscleMovesBetweenPrimaryAndSecondary() {
        val draft = ExerciseDraft().togglePrimary(MuscleGroup.CHEST).toggleSecondary(MuscleGroup.CHEST)
        assertEquals(emptySet<MuscleGroup>(), draft.primaryMuscles)
        assertEquals(setOf(MuscleGroup.CHEST), draft.secondaryMuscles)
        assertEquals(emptySet<MuscleGroup>(), draft.toggleSecondary(MuscleGroup.CHEST).secondaryMuscles)
    }

    @Test
    fun roundTripFromStoredExercise() {
        val stored = Exercise(
            id = 7,
            name = "Bankdrücken",
            type = ExerciseType.STRENGTH,
            equipment = Equipment.BARBELL,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            repRangeMin = 6,
            repRangeMax = 8,
            incrementKg = 2.5,
            restSeconds = 180,
        )
        val draft = ExerciseDraft.from(stored)
        assertEquals("2.5", draft.increment)
        assertEquals(stored, draft.toExercise())
    }

    @Test
    fun formatsNumbersWithoutTrailingZero() {
        assertEquals("5", ExerciseDraft.formatNumber(5.0))
        assertEquals("2.5", ExerciseDraft.formatNumber(2.5))
        assertEquals("11.02", ExerciseDraft.formatNumber(11.0231))
    }
}
