package io.github.wtfjb.aximo.ui.exercises

import androidx.annotation.StringRes
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.units.WeightUnit

// Display names for the domain enums. The domain stays free of Android resources.

@StringRes
fun ExerciseType.label(): Int = when (this) {
    ExerciseType.STRENGTH -> R.string.exercise_type_strength
    ExerciseType.BODYWEIGHT -> R.string.exercise_type_bodyweight
    ExerciseType.CARDIO -> R.string.exercise_type_cardio
}

@StringRes
fun Equipment.label(): Int = when (this) {
    Equipment.BARBELL -> R.string.equipment_barbell
    Equipment.DUMBBELL -> R.string.equipment_dumbbell
    Equipment.MACHINE -> R.string.equipment_machine
    Equipment.CABLE -> R.string.equipment_cable
    Equipment.KETTLEBELL -> R.string.equipment_kettlebell
    Equipment.BAND -> R.string.equipment_band
    Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
    Equipment.OTHER -> R.string.equipment_other
}

@StringRes
fun BodyRegion.label(): Int = when (this) {
    BodyRegion.CHEST -> R.string.region_chest
    BodyRegion.BACK -> R.string.region_back
    BodyRegion.LEGS -> R.string.region_legs
    BodyRegion.SHOULDERS -> R.string.region_shoulders
    BodyRegion.ARMS -> R.string.region_arms
    BodyRegion.CORE -> R.string.region_core
}

@StringRes
fun MuscleGroup.label(): Int = when (this) {
    MuscleGroup.CHEST -> R.string.muscle_chest
    MuscleGroup.UPPER_BACK -> R.string.muscle_upper_back
    MuscleGroup.LATS -> R.string.muscle_lats
    MuscleGroup.LOWER_BACK -> R.string.muscle_lower_back
    MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
    MuscleGroup.BICEPS -> R.string.muscle_biceps
    MuscleGroup.TRICEPS -> R.string.muscle_triceps
    MuscleGroup.FOREARMS -> R.string.muscle_forearms
    MuscleGroup.ABS -> R.string.muscle_abs
    MuscleGroup.QUADS -> R.string.muscle_quads
    MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
    MuscleGroup.GLUTES -> R.string.muscle_glutes
    MuscleGroup.CALVES -> R.string.muscle_calves
    MuscleGroup.ADDUCTORS -> R.string.muscle_adductors
}

@StringRes
fun WeightUnit.label(): Int = when (this) {
    WeightUnit.KG -> R.string.unit_kg
    WeightUnit.LBS -> R.string.unit_lbs
}
