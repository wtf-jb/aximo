package io.github.wtfjb.aximo.domain.logging

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.catalog.CatalogSearch
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating

/** An exercise name sent to the AI so it can write names the way the user's list does. */
data class LoggingExercise(val name: String, val equipment: Equipment)

/**
 * Exactly what is sent to the AI for B-04: the typed or spoken text, the names of
 * the user's exercises and the two display settings that decide what a bare number means.
 */
data class LoggingInput(
    val text: String,
    val exercises: List<LoggingExercise>,
    val unit: WeightUnit,
    val rating: SetRating,
) {
    companion object {
        const val MAX_TEXT = 1_000

        /** Sent at most; the list is sorted by name. */
        const val MAX_EXERCISES = 200

        /** Strength and bodyweight exercises that are not archived. */
        fun from(text: String, all: List<Exercise>, settings: TrainingSettings): LoggingInput = LoggingInput(
            text = text.trim().take(MAX_TEXT),
            exercises = all
                .filter { !it.archived && it.type != ExerciseType.CARDIO }
                .sortedBy { it.name.lowercase() }
                .take(MAX_EXERCISES)
                .map { LoggingExercise(it.name, it.equipment) },
            unit = settings.unit,
            rating = settings.rating,
        )
    }
}

/** A group of identical sets as the AI read it: "3x8 80 kg" is one group with [count] 3. */
data class ParsedSetGroup(
    val count: Int,
    val weight: Double?,
    /** Unit the user named; null = the display unit from the settings. */
    val weightUnit: WeightUnit?,
    val reps: Int,
    val rir: Int?,
    val rpe: Double?,
    val setType: SetType,
)

data class ParsedExercise(
    /** The name as the AI wrote it (the user's list spelling if it recognised one). */
    val name: String,
    /** Hints for a new exercise; null = not said. */
    val type: ExerciseType?,
    val equipment: Equipment?,
    val sets: List<ParsedSetGroup>,
)

/** What the AI answered for B-04: matches the schema but is not yet checked against value ranges. */
data class ParsedLog(
    val exercises: List<ParsedExercise>,
    /** Entries that did not match the schema. */
    val dropped: Int = 0,
)

/** Builds the prompt, calls the provider and parses the JSON answer; lives in `:ai`. */
fun interface LoggingGenerator {
    /** [language] is the app language as a tag ("de", "en"); new exercise names are written in it. */
    suspend fun parse(provider: AiProvider, input: LoggingInput, language: String): ParsedLog
}

/** Why a text could not be turned into sets, besides [io.github.wtfjb.aximo.domain.ai.AiException]. */
class LoggingException(val reason: Reason) : Exception("Logging not possible: $reason") {
    enum class Reason { NO_PROFILE, EMPTY_TEXT, NOTHING_USABLE }
}

/** A checked set of the preview. [weightKg] is null when the text gave no weight. */
data class ProposedSet(
    val weightKg: Double?,
    val reps: Int,
    val rir: Int?,
    val rpe: Double?,
    val setType: SetType,
)

/** [count] identical sets in a row, for a compact preview ("3 × 80 × 8"). */
data class SetRun(val count: Int, val set: ProposedSet)

/** An exercise that does not exist yet and is only created when the user saves. */
data class NewExercise(
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    /** Library entry that fits the name; muscles, rep range and instructions come from it. */
    val catalogEntry: CatalogEntry?,
) {
    fun toExercise(settings: TrainingSettings): Exercise =
        catalogEntry?.let { CatalogSearch.exerciseFor(it, settings).copy(name = name) } ?: Exercise(
            name = name,
            type = type,
            equipment = equipment,
            incrementKg = settings.steps.forEquipment(equipment),
            restSeconds = settings.restSeconds,
        )
}

/** What a spoken exercise can be assigned to. */
sealed interface ExerciseChoice {
    data class Existing(val exercise: Exercise) : ExerciseChoice
    data class Create(val exercise: NewExercise) : ExerciseChoice
}

/** One exercise of the preview: what the user said, the ways to assign it and its sets. */
data class ProposedLogEntry(
    val spoken: String,
    val options: List<ExerciseChoice>,
    /** Index into [options]; null while the user still has to choose. */
    val selected: Int?,
    val sets: List<ProposedSet>,
    val included: Boolean = true,
) {
    val choice: ExerciseChoice? get() = selected?.let { options.getOrNull(it) }

    /** The sets with consecutive identical ones merged. */
    fun runs(): List<SetRun> = sets.fold(emptyList()) { runs, set ->
        val last = runs.lastOrNull()
        if (last != null && last.set == set) runs.dropLast(1) + last.copy(count = last.count + 1) else runs + SetRun(1, set)
    }

    /** A weighted exercise whose text gave no weight; saved as 0 kg. */
    val weightMissing: Boolean
        get() {
            val type = when (val c = choice) {
                is ExerciseChoice.Existing -> c.exercise.type
                is ExerciseChoice.Create -> c.exercise.type
                null -> return false
            }
            return type != ExerciseType.BODYWEIGHT && sets.any { it.weightKg == null }
        }
}

/** The preview the user confirms before anything is saved. */
data class LoggingProposal(
    val entries: List<ProposedLogEntry>,
    /** Entries or sets of the answer that were dropped because they did not fit. */
    val dropped: Int = 0,
) {
    /** Entries that will be saved. */
    val confirmed: List<ProposedLogEntry> get() = entries.filter { it.included && it.selected != null }

    /** An included entry still waits for the choice of an exercise. */
    val needsChoice: Boolean get() = entries.any { it.included && it.selected == null }

    val canSave: Boolean get() = confirmed.isNotEmpty() && !needsChoice

    fun setIncluded(index: Int, included: Boolean): LoggingProposal = change(index) { it.copy(included = included) }

    fun select(index: Int, option: Int): LoggingProposal =
        change(index) { if (option in it.options.indices) it.copy(selected = option) else it }

    private fun change(index: Int, transform: (ProposedLogEntry) -> ProposedLogEntry): LoggingProposal =
        if (index in entries.indices) copy(entries = entries.mapIndexed { i, e -> if (i == index) transform(e) else e }) else this
}
