package io.github.wtfjb.aximo.domain.review

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises

/**
 * Checks and applies [SuggestionChange]s to a routine (B-02). Pure logic: the
 * caller saves the result after the user tapped "Übernehmen".
 */
object SuggestionApplier {
    val SETS = 1..10
    val REPS = 1..50
    val RIR = 0..5

    /**
     * True if [change] fits the routine as it is now: the exercise is (or for
     * an addition is not yet) in it, the `from` values still match and the new
     * values are in range.
     */
    fun isApplicable(change: SuggestionChange, routine: RoutineWithExercises?, exercises: List<Exercise>): Boolean {
        if (routine == null || routine.routine.id != change.routineId) return false
        val current = routine.exercises.firstOrNull { it.exerciseId == change.exerciseId }
        return when (change) {
            is SuggestionChange.SetCount ->
                current != null && current.targetSets == change.from && change.to in SETS && change.to != change.from
            is SuggestionChange.RepRange ->
                current != null && current.repMin == change.fromMin && current.repMax == change.fromMax &&
                    change.toMin in REPS && change.toMax in REPS && change.toMin <= change.toMax &&
                    (change.toMin != change.fromMin || change.toMax != change.fromMax)
            is SuggestionChange.TargetRir ->
                current != null && current.targetRir == change.from && (change.to == null || change.to in RIR) && change.to != change.from
            is SuggestionChange.AddExercise -> {
                val exercise = exercises.firstOrNull { it.id == change.exerciseId }
                current == null && exercise != null && !exercise.archived && exercise.type != ExerciseType.CARDIO &&
                    change.sets in SETS && change.repMin in REPS && change.repMax in REPS && change.repMin <= change.repMax &&
                    (change.targetRir == null || change.targetRir in RIR)
            }
            is SuggestionChange.RemoveExercise -> current != null && routine.exercises.size > 1
        }
    }

    /**
     * The routine's exercises after [change]. Call only if [isApplicable].
     * A removed exercise also leaves its superset; a superset of one is dissolved.
     */
    fun apply(change: SuggestionChange, routine: RoutineWithExercises): List<RoutineExercise> {
        val list = routine.exercises
        val result = when (change) {
            is SuggestionChange.SetCount -> list.updateFirst(change.exerciseId) { it.copy(targetSets = change.to) }
            is SuggestionChange.RepRange -> list.updateFirst(change.exerciseId) { it.copy(repMin = change.toMin, repMax = change.toMax) }
            is SuggestionChange.TargetRir -> list.updateFirst(change.exerciseId) { it.copy(targetRir = change.to) }
            is SuggestionChange.AddExercise -> list + RoutineExercise(
                routineId = routine.routine.id,
                exerciseId = change.exerciseId,
                position = list.size,
                targetSets = change.sets,
                repMin = change.repMin,
                repMax = change.repMax,
                targetRir = change.targetRir,
            )
            is SuggestionChange.RemoveExercise -> {
                val index = list.indexOfFirst { it.exerciseId == change.exerciseId }
                list.filterIndexed { i, _ -> i != index }
            }
        }
        return result.dissolveSingleSupersets().mapIndexed { index, e -> e.copy(position = index) }
    }

    private fun List<RoutineExercise>.updateFirst(exerciseId: Long, update: (RoutineExercise) -> RoutineExercise): List<RoutineExercise> {
        val index = indexOfFirst { it.exerciseId == exerciseId }
        return mapIndexed { i, e -> if (i == index) update(e) else e }
    }

    private fun List<RoutineExercise>.dissolveSingleSupersets(): List<RoutineExercise> {
        val sizes = groupingBy { it.supersetGroup }.eachCount()
        return map { if (it.supersetGroup != null && sizes[it.supersetGroup] == 1) it.copy(supersetGroup = null) else it }
    }
}
