package io.github.wtfjb.aximo.domain.exercise

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.units.WeightUnit

/** A problem with one field of the exercise form. */
enum class ExerciseFieldError {
    NAME_MISSING,
    REP_RANGE_INVALID,
    INCREMENT_INVALID,
    REST_INVALID,
}

/**
 * The exercise form as the user typed it (numbers as text). [validate] checks it
 * and [toExercise] turns it into an [Exercise]. The increment is entered in
 * [unit] and stored in kg.
 */
data class ExerciseDraft(
    val id: Long = 0,
    val name: String = "",
    val type: ExerciseType = ExerciseType.STRENGTH,
    val equipment: Equipment = Equipment.BARBELL,
    val primaryMuscles: Set<MuscleGroup> = emptySet(),
    val secondaryMuscles: Set<MuscleGroup> = emptySet(),
    val repRangeMin: String = Exercise.DEFAULT_REP_RANGE_MIN.toString(),
    val repRangeMax: String = Exercise.DEFAULT_REP_RANGE_MAX.toString(),
    val increment: String = formatNumber(Exercise.DEFAULT_INCREMENT_KG),
    val restSeconds: String = Exercise.DEFAULT_REST_SECONDS.toString(),
    val note: String = "",
    val archived: Boolean = false,
    val unit: WeightUnit = WeightUnit.KG,
) {
    /** Selecting a muscle as primary removes it from secondary and the other way round. */
    fun togglePrimary(muscle: MuscleGroup): ExerciseDraft =
        if (muscle in primaryMuscles) {
            copy(primaryMuscles = primaryMuscles - muscle)
        } else {
            copy(primaryMuscles = primaryMuscles + muscle, secondaryMuscles = secondaryMuscles - muscle)
        }

    fun toggleSecondary(muscle: MuscleGroup): ExerciseDraft =
        if (muscle in secondaryMuscles) {
            copy(secondaryMuscles = secondaryMuscles - muscle)
        } else {
            copy(secondaryMuscles = secondaryMuscles + muscle, primaryMuscles = primaryMuscles - muscle)
        }

    fun validate(): Set<ExerciseFieldError> {
        val errors = mutableSetOf<ExerciseFieldError>()
        if (name.isBlank()) errors += ExerciseFieldError.NAME_MISSING
        val min = repRangeMin.trim().toIntOrNull()
        val max = repRangeMax.trim().toIntOrNull()
        if (min == null || max == null || min < 1 || min > max) errors += ExerciseFieldError.REP_RANGE_INVALID
        val inc = parseNumber(increment)
        if (inc == null || inc < 0) errors += ExerciseFieldError.INCREMENT_INVALID
        val rest = restSeconds.trim().toIntOrNull()
        if (rest == null || rest < 0) errors += ExerciseFieldError.REST_INVALID
        return errors
    }

    /** Returns the exercise, or null if [validate] finds errors. */
    fun toExercise(): Exercise? {
        if (validate().isNotEmpty()) return null
        return Exercise(
            id = id,
            name = name.trim(),
            type = type,
            equipment = equipment,
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles,
            note = note.trim(),
            incrementKg = unit.toKg(parseNumber(increment)!!),
            repRangeMin = repRangeMin.trim().toInt(),
            repRangeMax = repRangeMax.trim().toInt(),
            restSeconds = restSeconds.trim().toInt(),
            archived = archived,
        )
    }

    companion object {
        /** Fills the form from a stored exercise. Fields the form doesn't show are kept by the caller. */
        fun from(exercise: Exercise, unit: WeightUnit = WeightUnit.KG) = ExerciseDraft(
            id = exercise.id,
            name = exercise.name,
            type = exercise.type,
            equipment = exercise.equipment,
            primaryMuscles = exercise.primaryMuscles,
            secondaryMuscles = exercise.secondaryMuscles,
            repRangeMin = exercise.repRangeMin.toString(),
            repRangeMax = exercise.repRangeMax.toString(),
            increment = formatNumber(unit.fromKg(exercise.incrementKg)),
            restSeconds = exercise.restSeconds.toString(),
            note = exercise.note,
            archived = exercise.archived,
            unit = unit,
        )

        /** Accepts "2,5" and "2.5". */
        fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

        /** 2.5 → "2.5", 5.0 → "5". The UI shows the decimal separator of the current locale. */
        fun formatNumber(value: Double): String {
            val rounded = Math.round(value * 100) / 100.0
            return if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
        }
    }
}
