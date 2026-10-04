package io.github.wtfjb.aximo.domain.chat

import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.catalog.CatalogSearch
import io.github.wtfjb.aximo.domain.exercise.normalizeSearch
import io.github.wtfjb.aximo.domain.logging.NewExercise
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.plan.PlanBuilder
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.PlanRoutine
import io.github.wtfjb.aximo.domain.review.SuggestionApplier
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.settings.TrainingSettings

/**
 * New plans from the chat (B-05): cleans up what the AI sent and resolves
 * exercises the user does not have yet against the bundled library, locally,
 * so the library never has to be sent.
 */
object PlanSuggestions {

    /** A cleaned plan and how many routines or entries were dropped. */
    data class Resolved(val plan: SuggestionChange.CreatePlan?, val dropped: Int)

    fun resolve(plan: SuggestionChange.CreatePlan, exercises: List<Exercise>, catalog: List<CatalogEntry>): Resolved {
        var dropped = 0
        val routines = plan.routines.mapNotNull { routine ->
            val name = routine.name.trim().take(PlanBuilder.MAX_NAME_LENGTH)
            val seen = mutableSetOf<Any>()
            val entries = routine.exercises.mapNotNull { entry ->
                val resolved = entry.copy(exercise = resolve(entry.exercise, exercises, catalog))
                val key = when (val ref = resolved.exercise) {
                    is PlanExerciseRef.Existing -> ref.exerciseId
                    is PlanExerciseRef.New -> normalizeSearch(ref.name)
                }
                if (SuggestionApplier.isValidEntry(resolved, exercises) && seen.add(key)) {
                    resolved
                } else {
                    dropped++
                    null
                }
            }.take(PlanBuilder.MAX_EXERCISES_PER_ROUTINE)
            if (name.isEmpty() || entries.isEmpty()) {
                dropped++
                null
            } else {
                PlanRoutine(name, entries)
            }
        }
        val kept = routines.take(SuggestionApplier.MAX_PLAN_ROUTINES)
        dropped += routines.size - kept.size
        return Resolved(kept.takeIf { it.isNotEmpty() }?.let { SuggestionChange.CreatePlan(it) }, dropped)
    }

    /**
     * An own exercise with the same name or library entry wins; otherwise the
     * library entry that fits the English name (or the display name) is attached.
     */
    private fun resolve(ref: PlanExerciseRef, exercises: List<Exercise>, catalog: List<CatalogEntry>): PlanExerciseRef {
        if (ref !is PlanExerciseRef.New) return ref
        val name = ref.name.trim()
        val usable = exercises.filter { !it.archived && it.type != ExerciseType.CARDIO }
        usable.firstOrNull { normalizeSearch(it.name) == normalizeSearch(name) }?.let { return PlanExerciseRef.Existing(it.id) }
        val entry = listOfNotNull(ref.catalogName, name).firstNotNullOfOrNull { query ->
            CatalogSearch.filter(catalog, query, null).firstOrNull()
        }
        entry?.let { e -> usable.firstOrNull { it.catalogId == e.catalogId }?.let { return PlanExerciseRef.Existing(it.id) } }
        return ref.copy(
            name = name.replaceFirstChar { it.uppercase() },
            type = entry?.type ?: ref.type,
            equipment = entry?.equipment ?: ref.equipment,
            catalogId = entry?.catalogId,
        )
    }

    /**
     * The exercise for a new entry, as it would be created on save: muscles,
     * rep range and instructions from the library entry if there is one.
     */
    fun newExercise(ref: PlanExerciseRef.New, catalog: List<CatalogEntry>, settings: TrainingSettings): Exercise =
        NewExercise(ref.name, ref.type, ref.equipment, CatalogSearch.entryFor(catalog, ref.catalogId)).toExercise(settings)

    /** The routines that are saved: all, minus the ones the user switched off. */
    fun selected(plan: SuggestionChange.CreatePlan, excluded: Set<Int>): List<PlanRoutine> =
        plan.routines.filterIndexed { index, _ -> index !in excluded }
}
