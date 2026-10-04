package io.github.wtfjb.aximo.domain.settings

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating

/**
 * Default weight steps (A-09), in kg. [dumbbellKg] applies to dumbbells, [barbellKg]
 * to everything else. Used as increment of new exercises and as the plate step the
 * progression rounds to when an exercise has no own rounding step.
 */
data class WeightSteps(val barbellKg: Double, val dumbbellKg: Double) {
    init {
        require(barbellKg > 0 && dumbbellKg > 0) { "weight steps must be positive" }
    }

    fun forEquipment(equipment: Equipment): Double =
        if (equipment == Equipment.DUMBBELL) dumbbellKg else barbellKg

    companion object {
        /** 2.5 kg / 2 kg dumbbells, or 5 lbs for both. */
        fun standard(unit: WeightUnit): WeightSteps = when (unit) {
            WeightUnit.KG -> WeightSteps(barbellKg = 2.5, dumbbellKg = 2.0)
            WeightUnit.LBS -> WeightSteps(barbellKg = unit.toKg(5.0), dumbbellKg = unit.toKg(5.0))
        }
    }
}

/**
 * Training settings (A-09). Weights are stored in kg; [unit] only changes the display.
 * [rating] is the scale for set effort (A-02).
 */
data class TrainingSettings(
    val unit: WeightUnit = WeightUnit.KG,
    val restSeconds: Int = Exercise.DEFAULT_REST_SECONDS,
    val steps: WeightSteps = WeightSteps.standard(unit),
    val rating: SetRating = SetRating.RIR,
) {
    init {
        require(restSeconds >= 0) { "restSeconds must not be negative" }
    }

    /**
     * Switches the display unit. Steps that are still the standard of the old unit
     * move to the standard of the new one (2.5 kg → 5 lbs); custom steps stay.
     */
    fun withUnit(newUnit: WeightUnit): TrainingSettings {
        if (newUnit == unit) return this
        val newSteps = if (steps == WeightSteps.standard(unit)) WeightSteps.standard(newUnit) else steps
        return copy(unit = newUnit, steps = newSteps)
    }

    companion object {
        /** Choices for the default rest, in seconds. */
        val REST_CHOICES = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)
    }
}
