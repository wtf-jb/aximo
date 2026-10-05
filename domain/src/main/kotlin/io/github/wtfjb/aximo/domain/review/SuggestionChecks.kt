package io.github.wtfjb.aximo.domain.review

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Exercise

/**
 * Catches suggestions that contradict the numbers the app itself sent (review
 * and chat). Runs after [SuggestionApplier.applicable]; dropped suggestions are
 * counted like invalid ones. Rules: docs/decisions.md, "Coach-Vorschläge".
 */
object SuggestionChecks {

    fun plausible(suggestions: List<GeneratedSuggestion>, context: ReviewContext, exercises: List<Exercise>): List<GeneratedSuggestion> =
        suggestions.filter { isPlausible(it.change, context, exercises) }

    private fun isPlausible(change: SuggestionChange, context: ReviewContext, exercises: List<Exercise>): Boolean {
        val status = context.volume.associate { it.region to it.status }
        fun regionsOf(exerciseId: Long): List<BodyRegion> =
            exercises.firstOrNull { it.id == exerciseId }?.primaryMuscles?.map { it.region }.orEmpty()

        return when (change) {
            is SuggestionChange.SetCount -> {
                val regions = regionsOf(change.exerciseId)
                if (change.to < change.from) {
                    val trend = context.exercises.firstOrNull { it.exerciseId == change.exerciseId }
                    val fatigued = trend?.status == TrendStatus.REGRESSING || trend?.effort == EffortStatus.TOO_HARD
                    fatigued || regions.none { status[it] == VolumeStatus.BELOW }
                } else {
                    regions.none { status[it] == VolumeStatus.ABOVE }
                }
            }
            is SuggestionChange.AddExercise -> {
                val regions = regionsOf(change.exerciseId)
                regions.isEmpty() || !regions.all { status[it] == VolumeStatus.ABOVE }
            }
            else -> true
        }
    }
}
