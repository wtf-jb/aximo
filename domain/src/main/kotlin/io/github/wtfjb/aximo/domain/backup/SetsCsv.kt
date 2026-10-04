package io.github.wtfjb.aximo.domain.backup

import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail

/**
 * CSV export of all logged sets for external analysis (A-08). One row per set,
 * oldest workout first. Comma-separated, "." as decimal separator, weights in kg,
 * times in UTC (ISO 8601), so spreadsheets and scripts read it the same everywhere.
 */
object SetsCsv {

    val HEADER = listOf(
        "workout_id", "started_at", "routine", "exercise", "exercise_position",
        "set_position", "set_type", "weight_kg", "reps", "rir", "rpe", "completed_at",
    )

    fun build(workouts: List<WorkoutDetail>, routines: List<Routine>): String {
        val routineNames = routines.associate { it.id to it.name }
        val lines = mutableListOf(HEADER.joinToString(","))
        for (detail in workouts.sortedBy { it.workout.startedAt }) {
            val workout = detail.workout
            for (exercise in detail.exercises.sortedBy { it.entry.position }) {
                for (set in exercise.sets.sortedBy { it.position }) {
                    val fields = listOf(
                        workout.id.toString(),
                        workout.startedAt.toString(),
                        workout.routineId?.let { routineNames[it] }.orEmpty(),
                        exercise.exercise.name,
                        (exercise.entry.position + 1).toString(),
                        (set.position + 1).toString(),
                        set.setType.name,
                        set.weightKg.toString(),
                        set.reps.toString(),
                        set.rir?.toString().orEmpty(),
                        set.rpe?.toString().orEmpty(),
                        set.completedAt?.toString().orEmpty(),
                    )
                    lines += fields.joinToString(",") { escape(it) }
                }
            }
        }
        return lines.joinToString("\n", postfix = "\n")
    }

    /** Quotes a field that contains a comma, quote or line break; quotes inside are doubled. */
    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
