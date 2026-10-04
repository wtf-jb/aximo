package io.github.wtfjb.aximo.domain.exercise

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Exercise

/**
 * Filter for the exercise list: free-text search plus an optional body region.
 * A region matches if one of the exercise's primary muscles lies in it.
 */
data class ExerciseFilter(
    val query: String = "",
    val region: BodyRegion? = null,
) {
    fun matches(exercise: Exercise): Boolean {
        val words = normalizeSearch(query).split(' ').filter { it.isNotEmpty() }
        val name = normalizeSearch(exercise.name)
        val queryMatches = words.all { it in name }
        val regionMatches = region == null || exercise.primaryMuscles.any { it.region == region }
        return queryMatches && regionMatches
    }

    fun apply(exercises: List<Exercise>): List<Exercise> = exercises.filter(::matches)
}

/** Lower case and German umlauts folded, so "bankdrucken" finds "Bankdrücken". */
internal fun normalizeSearch(text: String): String = text.trim().lowercase()
    .replace('ä', 'a').replace('ö', 'o').replace('ü', 'u').replace("ß", "ss")
