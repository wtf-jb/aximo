package io.github.wtfjb.aximo.domain.review

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import kotlin.time.Instant

/**
 * One change the AI proposes (B-02, B-05). Exercises are addressed by routine and
 * exercise id: routine exercise ids change on every save. The `from` values say
 * what the AI saw; if the routine changed since, the suggestion no longer applies.
 */
sealed interface SuggestionChange {

    /** A change to one existing routine. */
    sealed interface RoutineChange : SuggestionChange {
        val routineId: Long
    }

    /** A change to one exercise of an existing routine. */
    sealed interface ExerciseChange : RoutineChange {
        val exerciseId: Long
    }

    data class SetCount(override val routineId: Long, override val exerciseId: Long, val from: Int, val to: Int) : ExerciseChange

    data class RepRange(
        override val routineId: Long,
        override val exerciseId: Long,
        val fromMin: Int,
        val fromMax: Int,
        val toMin: Int,
        val toMax: Int,
    ) : ExerciseChange

    data class TargetRir(override val routineId: Long, override val exerciseId: Long, val from: Int?, val to: Int?) : ExerciseChange

    data class AddExercise(
        override val routineId: Long,
        override val exerciseId: Long,
        val sets: Int,
        val repMin: Int,
        val repMax: Int,
        val targetRir: Int?,
    ) : ExerciseChange

    data class RemoveExercise(override val routineId: Long, override val exerciseId: Long) : ExerciseChange

    /** Swaps [exerciseId] for [newExerciseId]; sets, reps and RIR stay (B-05). */
    data class ReplaceExercise(override val routineId: Long, override val exerciseId: Long, val newExerciseId: Long) : ExerciseChange

    /** Moves the exercise from index [from] to index [to] (0-based) within the routine (B-05). */
    data class MoveExercise(override val routineId: Long, override val exerciseId: Long, val from: Int, val to: Int) : ExerciseChange

    data class RenameRoutine(override val routineId: Long, val from: String, val to: String) : RoutineChange

    /** Deletes the routine; finished workouts of it stay in the history. */
    data class DeleteRoutine(override val routineId: Long, val name: String) : RoutineChange

    /** New routines from the chat (B-05), saved at the end of the list after the user confirms. */
    data class CreatePlan(val routines: List<PlanRoutine>) : SuggestionChange
}

/** A routine of a [SuggestionChange.CreatePlan]. */
data class PlanRoutine(val name: String, val exercises: List<PlanEntry>)

data class PlanEntry(val exercise: PlanExerciseRef, val sets: Int, val repMin: Int, val repMax: Int, val targetRir: Int?)

/** Which exercise a plan entry uses: one of the user's, or a new one created on save. */
sealed interface PlanExerciseRef {
    data class Existing(val exerciseId: Long) : PlanExerciseRef

    /**
     * Not among the user's exercises yet. [catalogId] is the library entry that
     * fits (muscles, rep range, instructions come from it); [name] is the
     * display name in the app language.
     */
    data class New(
        val name: String,
        val type: ExerciseType,
        val equipment: Equipment,
        val catalogId: String? = null,
        /** The English library name the AI gave; used to find [catalogId]. */
        val catalogName: String? = null,
    ) : PlanExerciseRef
}

enum class SuggestionStatus { OPEN, APPLIED, DISCARDED }

/**
 * Why the AI proposes a change; the coach card shows it as a chip. Optional:
 * chat answers and stored suggestions from before may have none.
 */
enum class SuggestionReason {
    PROGRESS, STAGNATION, REGRESSION, RETURNING, VOLUME_LOW, VOLUME_HIGH, EFFORT_HIGH, EFFORT_LOW, REP_CEILING, OTHER
}

/**
 * A stored suggestion from a review or the coach chat (B-05). The AI never
 * changes data itself; only [SuggestionApplier] does, after the user confirms.
 */
data class AiSuggestion(
    val id: Long = 0,
    /** Set for suggestions of a weekly review. */
    val reviewId: Long? = null,
    /** Set for suggestions of a chat answer. */
    val chatMessageId: Long? = null,
    val change: SuggestionChange,
    /** Why, in the app language, naming the numbers ("9 Sätze, Ziel 10–20"). */
    val rationale: String,
    val status: SuggestionStatus = SuggestionStatus.OPEN,
    val reason: SuggestionReason? = null,
)

/** One weekly review: summary text plus its suggestions. */
data class AiReview(
    val id: Long = 0,
    val createdAt: Instant,
    val weeks: Int,
    val summary: String,
    val suggestions: List<AiSuggestion> = emptyList(),
    /** Suggestions the AI sent that did not pass validation and were dropped. */
    val droppedSuggestions: Int = 0,
)
