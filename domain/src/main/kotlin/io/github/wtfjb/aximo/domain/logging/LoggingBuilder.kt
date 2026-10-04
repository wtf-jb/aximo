package io.github.wtfjb.aximo.domain.logging

import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.catalog.CatalogSearch
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.workout.Effort
import io.github.wtfjb.aximo.domain.workout.SetRating

/**
 * Checks the AI's reading of the text against the value ranges of the app, expands
 * "3x8" into sets and assigns every name to an exercise (B-04). Whatever does not
 * fit is dropped and counted; the rest becomes the preview the user confirms.
 */
object LoggingBuilder {
    const val MAX_EXERCISES = 10
    const val MAX_GROUP_COUNT = 10
    const val MAX_SETS_PER_EXERCISE = 20
    const val MAX_REPS = 100
    const val MIN_WEIGHT_KG = -300.0
    const val MAX_WEIGHT_KG = 1_000.0
    const val MAX_RIR = 10
    const val MAX_NAME_LENGTH = 60

    /**
     * [exercises] are all exercises the name can be assigned to (not archived, no cardio);
     * [catalog] suggests muscles and type for a name that is new.
     * Throws [LoggingException] (NOTHING_USABLE) if nothing is left.
     */
    fun build(parsed: ParsedLog, exercises: List<Exercise>, catalog: List<CatalogEntry>, settings: TrainingSettings): LoggingProposal {
        var dropped = parsed.dropped
        val entries = parsed.exercises.mapNotNull { exercise ->
            val name = exercise.name.trim().take(MAX_NAME_LENGTH)
            val sets = expand(exercise.sets, settings) { dropped++ }
            if (name.isEmpty() || sets.isEmpty()) {
                dropped++
                null
            } else {
                entry(name, exercise, sets.take(MAX_SETS_PER_EXERCISE), exercises, catalog)
            }
        }
        val kept = entries.take(MAX_EXERCISES)
        dropped += entries.size - kept.size
        if (kept.isEmpty()) throw LoggingException(LoggingException.Reason.NOTHING_USABLE)
        return LoggingProposal(kept, dropped)
    }

    private fun entry(name: String, parsed: ParsedExercise, sets: List<ProposedSet>, exercises: List<Exercise>, catalog: List<CatalogEntry>): ProposedLogEntry {
        val create = { ExerciseChoice.Create(newExercise(name, parsed, catalog)) }
        return when (val match = ExerciseMatcher.match(name, exercises)) {
            is ExerciseMatch.Sure -> ProposedLogEntry(
                spoken = name,
                options = (listOf(match.exercise) + match.alternatives).map { ExerciseChoice.Existing(it) },
                selected = 0,
                sets = sets,
            )
            is ExerciseMatch.Unsure -> ProposedLogEntry(
                spoken = name,
                options = match.candidates.map { ExerciseChoice.Existing(it) } + create(),
                selected = null,
                sets = sets,
            )
            ExerciseMatch.None -> ProposedLogEntry(spoken = name, options = listOf(create()), selected = 0, sets = sets)
        }
    }

    private fun newExercise(name: String, parsed: ParsedExercise, catalog: List<CatalogEntry>): NewExercise {
        val entry = CatalogSearch.filter(catalog, name, null).firstOrNull()
        val type = entry?.type ?: parsed.type?.takeIf { it != ExerciseType.CARDIO } ?: ExerciseType.STRENGTH
        val equipment = entry?.equipment ?: parsed.equipment ?: if (type == ExerciseType.BODYWEIGHT) Equipment.BODYWEIGHT else Equipment.OTHER
        return NewExercise(name.replaceFirstChar { it.uppercase() }, type, equipment, entry)
    }

    private inline fun expand(groups: List<ParsedSetGroup>, settings: TrainingSettings, onDropped: () -> Unit): List<ProposedSet> =
        groups.flatMap { group ->
            val set = set(group, settings)
            if (set == null || group.count !in 1..MAX_GROUP_COUNT) {
                onDropped()
                emptyList()
            } else {
                List(group.count) { set }
            }
        }

    /** One set of a group in kg with one effort scale, or null if a value is out of range. */
    private fun set(group: ParsedSetGroup, settings: TrainingSettings): ProposedSet? {
        if (group.reps !in 1..MAX_REPS) return null
        val weightKg = group.weight?.let { (group.weightUnit ?: settings.unit).toKg(it) }
        if (weightKg != null && (weightKg < MIN_WEIGHT_KG || weightKg > MAX_WEIGHT_KG)) return null
        val rir = group.rir?.also { if (it !in 0..MAX_RIR) return null }
        val rpe = group.rpe?.let { Effort.parseRpe(it.toString()) ?: return null }
        // Both given: the scale from the settings wins. One scale is stored per set.
        val useRpe = rpe != null && (rir == null || settings.rating == SetRating.RPE)
        return ProposedSet(
            weightKg = weightKg,
            reps = group.reps,
            rir = if (useRpe) null else rir,
            rpe = if (useRpe) rpe else null,
            setType = group.setType,
        )
    }
}
