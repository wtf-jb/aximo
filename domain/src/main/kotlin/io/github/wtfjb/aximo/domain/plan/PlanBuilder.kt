package io.github.wtfjb.aximo.domain.plan

/**
 * Checks the AI's plan against the exercises that were offered and the value
 * ranges of the app (B-03). Whatever does not fit is dropped and counted; the
 * rest becomes the draft the user confirms.
 */
object PlanBuilder {
    const val MAX_NAME_LENGTH = 40
    const val MAX_EXERCISES_PER_ROUTINE = 12

    /** Throws [PlanException] (NOTHING_USABLE) if no routine is left. */
    fun build(generated: GeneratedPlan, input: PlanInput): PlanProposal {
        val offered = input.exercises.associateBy { it.exerciseId }
        var dropped = generated.dropped
        val checked = generated.routines
            .mapNotNull { routine ->
                val name = routine.name.trim().take(MAX_NAME_LENGTH)
                val seen = mutableSetOf<Long>()
                val exercises = routine.exercises.mapNotNull { e ->
                    val exercise = offered[e.exerciseId]
                    val valid = exercise != null && isValid(e) && seen.add(e.exerciseId)
                    if (!valid) dropped++
                    exercise?.takeIf { valid }?.let { ProposedExercise(it.exerciseId, it.name, e.sets, e.repMin, e.repMax, e.targetRir) }
                }.take(MAX_EXERCISES_PER_ROUTINE)
                if (name.isEmpty() || exercises.isEmpty()) {
                    dropped++
                    null
                } else {
                    ProposedRoutine(name, exercises)
                }
            }
        // More routines than days: keep the first ones.
        val routines = checked.take(input.request.daysPerWeek)
        dropped += checked.size - routines.size
        if (routines.isEmpty()) throw PlanException(PlanException.Reason.NOTHING_USABLE)
        return PlanProposal(generated.summary, routines, dropped)
    }

    private fun isValid(e: GeneratedPlanExercise): Boolean =
        e.sets in 1..10 && e.repMin in 1..50 && e.repMax in e.repMin..50 && (e.targetRir == null || e.targetRir in 0..5)
}
