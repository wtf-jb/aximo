package io.github.wtfjb.aximo.domain.logging

import io.github.wtfjb.aximo.domain.exercise.normalizeSearch
import io.github.wtfjb.aximo.domain.model.Exercise
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/** How a spoken exercise name relates to the user's exercises (B-04). */
sealed interface ExerciseMatch {
    /** One clear hit; [alternatives] are weaker hits the user may switch to. */
    data class Sure(val exercise: Exercise, val alternatives: List<Exercise>) : ExerciseMatch

    /** No clear winner: the best 2–3 hits, best first. */
    data class Unsure(val candidates: List<Exercise>) : ExerciseMatch

    /** Nothing close enough. */
    data object None : ExerciseMatch
}

/**
 * Local fuzzy match of a name from the user's text to their exercises (B-04). No AI:
 * case, umlauts and punctuation are ignored; partial words ("kniebeuge" for
 * "Kniebeugen", "bank" for "Bankdrücken") and small typos count.
 */
object ExerciseMatcher {
    /** From this score on a hit is taken without asking … */
    const val SURE_SCORE = 0.8

    /** … if the next best hit is at least this far behind. */
    const val SURE_LEAD = 0.1

    /** Below this a hit is not even offered. */
    const val MIN_SCORE = 0.5

    const val MAX_CANDIDATES = 3

    fun match(spoken: String, exercises: List<Exercise>): ExerciseMatch {
        val query = words(spoken)
        if (query.isEmpty()) return ExerciseMatch.None
        val hits = exercises
            .map { it to score(query, words(it.name)) }
            .filter { it.second >= MIN_SCORE }
            .sortedWith(compareByDescending<Pair<Exercise, Double>> { it.second }.thenBy { it.first.name.lowercase() })
            .take(MAX_CANDIDATES)
        val best = hits.firstOrNull() ?: return ExerciseMatch.None
        val lead = best.second - (hits.getOrNull(1)?.second ?: 0.0)
        val exact = best.second >= EXACT && lead > 0
        return if (exact || (best.second >= SURE_SCORE && lead >= SURE_LEAD)) {
            ExerciseMatch.Sure(best.first, hits.drop(1).map { it.first })
        } else {
            ExerciseMatch.Unsure(hits.map { it.first })
        }
    }

    /** 0..1: how well the words of a name cover the spoken words. 1 = same words. */
    fun score(spoken: String, name: String): Double = score(words(spoken), words(name))

    private const val EXACT = 0.999

    private fun score(query: List<String>, name: List<String>): Double {
        if (query.isEmpty() || name.isEmpty()) return 0.0
        val queryCoverage = query.map { q -> name.maxOf { wordSimilarity(q, it) } }.average()
        val nameCoverage = name.map { t -> query.maxOf { wordSimilarity(it, t) } }.average()
        // A name with extra words ("Bankdrücken Langhantel" for "Bankdrücken") is a bit less likely than an exact one.
        return queryCoverage * (0.7 + 0.3 * nameCoverage)
    }

    /** 0..1 for two normalized words. */
    private fun wordSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val shorter = min(a.length, b.length)
        val longer = max(a.length, b.length)
        if (shorter < 3) return 0.0
        val ratio = shorter.toDouble() / longer
        if (a.startsWith(b) || b.startsWith(a)) return 0.6 + 0.3 * ratio
        // German compounds: "drucken" in "bankdrucken".
        if (shorter >= 4 && (a.contains(b) || b.contains(a))) return 0.5 + 0.2 * ratio
        val allowed = when {
            longer >= 8 -> 2
            longer >= 4 -> 1
            else -> 0
        }
        val distance = editDistance(a, b)
        return if (distance in 1..allowed) 1.0 - distance.toDouble() / longer else 0.0
    }

    /** Normalized words: umlauts and accents folded, everything but letters and digits is a separator. */
    private fun words(text: String): List<String> {
        val folded = Normalizer.normalize(normalizeSearch(text), Normalizer.Form.NFD).filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        return folded.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("").split(' ').filter { it.isNotEmpty() }
    }

    /** Edit distance where swapping two neighbours costs 1 (a common typo). */
    private fun editDistance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }
}
