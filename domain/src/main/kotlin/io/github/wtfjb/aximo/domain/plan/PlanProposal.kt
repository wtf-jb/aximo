package io.github.wtfjb.aximo.domain.plan

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.model.RoutineExercise

/** What the AI answered for B-03, parsed against the schema but not yet checked against the exercises. */
data class GeneratedPlan(
    val summary: String,
    val routines: List<GeneratedRoutine>,
    /** Entries that did not match the schema. */
    val dropped: Int = 0,
)

data class GeneratedRoutine(val name: String, val exercises: List<GeneratedPlanExercise>)

data class GeneratedPlanExercise(val exerciseId: Long, val sets: Int, val repMin: Int, val repMax: Int, val targetRir: Int?)

/** Builds the prompt, calls the provider and parses the JSON answer; lives in `:ai`. */
fun interface PlanGenerator {
    /** [language] is the app language as a tag ("de", "en"); the summary and routine names are written in it. */
    suspend fun generate(provider: AiProvider, input: PlanInput, language: String): GeneratedPlan
}

/** Why a plan could not be created, besides [io.github.wtfjb.aximo.domain.ai.AiException]. */
class PlanException(val reason: Reason) : Exception("Plan not possible: $reason") {
    enum class Reason { NO_PROFILE, NOT_ENOUGH_EXERCISES, NOTHING_USABLE }
}

/** A checked exercise of a proposed routine. */
data class ProposedExercise(
    val exerciseId: Long,
    val name: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int?,
)

data class ProposedRoutine(val name: String, val exercises: List<ProposedExercise>) {
    fun toEntries(): List<RoutineExercise> = exercises.mapIndexed { index, e ->
        RoutineExercise(
            exerciseId = e.exerciseId,
            position = index,
            targetSets = e.sets,
            repMin = e.repMin,
            repMax = e.repMax,
            targetRir = e.targetRir,
        )
    }
}

/** The plan draft the user confirms before anything is saved. */
data class PlanProposal(
    val summary: String,
    val routines: List<ProposedRoutine>,
    /** Entries of the answer that were dropped because they did not fit. */
    val dropped: Int = 0,
) {
    fun without(index: Int): PlanProposal =
        if (index in routines.indices) copy(routines = routines.filterIndexed { i, _ -> i != index }) else this
}
