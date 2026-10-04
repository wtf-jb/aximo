package io.github.wtfjb.aximo.domain.units

/**
 * Display unit for weights. Weights are always stored in kg; this is only used
 * to convert for display and input (A-09).
 */
enum class WeightUnit {
    KG,
    LBS,
    ;

    /** Converts a stored kg value to this unit. */
    fun fromKg(kg: Double): Double = when (this) {
        KG -> kg
        LBS -> kg / KG_PER_LB
    }

    /** Converts a value entered in this unit to kg for storage. */
    fun toKg(value: Double): Double = when (this) {
        KG -> value
        LBS -> value * KG_PER_LB
    }

    companion object {
        /** Exact by definition of the international pound. */
        const val KG_PER_LB = 0.45359237
    }
}
