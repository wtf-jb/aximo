package io.github.wtfjb.aximo.data

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup

fun benchPress(id: Long = 0) = Exercise(
    id = id,
    name = "Bankdrücken",
    type = ExerciseType.STRENGTH,
    equipment = Equipment.BARBELL,
    primaryMuscles = setOf(MuscleGroup.CHEST),
    secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
    repRangeMin = 6,
    repRangeMax = 8,
)

fun pullUp() = Exercise(
    name = "Klimmzug",
    type = ExerciseType.BODYWEIGHT,
    equipment = Equipment.BODYWEIGHT,
    primaryMuscles = setOf(MuscleGroup.LATS),
    secondaryMuscles = setOf(MuscleGroup.BICEPS),
)

fun running() = Exercise(name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)
