package io.github.wtfjb.aximo.domain.review

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.plan.PlanBuilder

/**
 * Checks and applies [SuggestionChange]s (B-02, B-05). Pure logic: the caller
 * saves the result after the user tapped "Übernehmen".
 */
object SuggestionApplier {
    val SETS = 1..10
    val REPS = 1..50
    val RIR = 0..5

    /** Most routines in one new plan. */
    const val MAX_PLAN_ROUTINES = 7

    /** True if [change] fits the current routines and exercises. */
    fun isApplicable(change: SuggestionChange, routines: List<RoutineWithExercises>, exercises: List<Exercise>): Boolean = when (change) {
        is SuggestionChange.RoutineChange -> isApplicable(change, routines.firstOrNull { it.routine.id == change.routineId }, exercises)
        is SuggestionChange.CreatePlan -> isApplicable(change, exercises)
    }

    /**
     * True if [change] fits the routine as it is now: the exercise is (or for
     * an addition is not yet) in it, the `from` values still match and the new
     * values are in range.
     */
    fun isApplicable(change: SuggestionChange.RoutineChange, routine: RoutineWithExercises?, exercises: List<Exercise>): Boolean {
        if (routine == null || routine.routine.id != change.routineId) return false
        return when (change) {
            is SuggestionChange.RenameRoutine -> {
                val to = change.to.trim()
                routine.routine.name == change.from && to.isNotEmpty() && to.length <= PlanBuilder.MAX_NAME_LENGTH && to != change.from
            }
            is SuggestionChange.DeleteRoutine -> routine.routine.name == change.name
            is SuggestionChange.ExerciseChange -> isApplicable(change, routine, exercises)
        }
    }

    private fun isApplicable(change: SuggestionChange.ExerciseChange, routine: RoutineWithExercises, exercises: List<Exercise>): Boolean {
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
            is SuggestionChange.AddExercise ->
                current == null && usable(change.exerciseId, exercises) &&
                    change.sets in SETS && change.repMin in REPS && change.repMax in REPS && change.repMin <= change.repMax &&
                    (change.targetRir == null || change.targetRir in RIR)
            is SuggestionChange.RemoveExercise -> current != null && routine.exercises.size > 1
            is SuggestionChange.ReplaceExercise ->
                current != null && change.newExerciseId != change.exerciseId && usable(change.newExerciseId, exercises) &&
                    routine.exercises.none { it.exerciseId == change.newExerciseId }
            is SuggestionChange.MoveExercise ->
                routine.exercises.indexOfFirst { it.exerciseId == change.exerciseId }.let { it >= 0 && it == change.from } &&
                    change.to in routine.exercises.indices && change.to != change.from
        }
    }

    /** A plan fits while it has routines and its existing exercises are still there (not archived, not cardio). */
    fun isApplicable(change: SuggestionChange.CreatePlan, exercises: List<Exercise>): Boolean =
        change.routines.isNotEmpty() && change.routines.size <= MAX_PLAN_ROUTINES &&
            change.routines.all { routine -> routine.exercises.isNotEmpty() && routine.exercises.all { isValidEntry(it, exercises) } }

    /** One plan entry: a usable or new non-cardio exercise and targets in range. */
    fun isValidEntry(entry: PlanEntry, exercises: List<Exercise>): Boolean {
        val exerciseOk = when (val ref = entry.exercise) {
            is PlanExerciseRef.Existing -> usable(ref.exerciseId, exercises)
            is PlanExerciseRef.New -> ref.name.isNotBlank() && ref.type != ExerciseType.CARDIO
        }
        return exerciseOk && entry.sets in SETS && entry.repMin in REPS && entry.repMax in REPS && entry.repMin <= entry.repMax &&
            (entry.targetRir == null || entry.targetRir in RIR)
    }

    private fun usable(exerciseId: Long, exercises: List<Exercise>): Boolean {
        val exercise = exercises.firstOrNull { it.id == exerciseId }
        return exercise != null && !exercise.archived && exercise.type != ExerciseType.CARDIO
    }

    /** The suggestions that fit the current routines, without duplicates (review and chat). */
    fun applicable(
        suggestions: List<GeneratedSuggestion>,
        routines: List<RoutineWithExercises>,
        exercises: List<Exercise>,
    ): List<GeneratedSuggestion> = suggestions
        .filter { isApplicable(it.change, routines, exercises) }
        .distinctBy { it.change }

    /**
     * The routine's exercises after [change]. Call only if [isApplicable].
     * A removed or moved exercise leaves its superset; a superset of one is dissolved.
     */
    fun apply(change: SuggestionChange.ExerciseChange, routine: RoutineWithExercises): List<RoutineExercise> {
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
            is SuggestionChange.ReplaceExercise -> list.updateFirst(change.exerciseId) { it.copy(exerciseId = change.newExerciseId) }
            is SuggestionChange.MoveExercise -> {
                val moved = list[change.from].copy(supersetGroup = null)
                list.toMutableList().apply {
                    removeAt(change.from)
                    add(change.to, moved)
                }
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
