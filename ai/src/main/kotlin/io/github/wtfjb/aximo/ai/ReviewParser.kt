package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.review.GeneratedReview
import io.github.wtfjb.aximo.ai.JsonRead.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Validates the review answer against the schema in [ReviewPrompt] (B-02).
 * The summary is required; a suggestion that does not match is dropped and
 * counted, the rest is kept. Whether ids and `from` values fit the routines is
 * checked later in the domain ([io.github.wtfjb.aximo.domain.review.SuggestionApplier]).
 */
object ReviewParser {

    fun parse(text: String): GeneratedReview {
        val root = JsonRead.extractObject(text) ?: throw JsonRead.invalid(text)
        val summary = root.string("summary")?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw JsonRead.invalid(text)
        val raw = (root["suggestions"] as? JsonArray).orEmpty()
        val parsed = raw.take(ReviewPrompt.MAX_SUGGESTIONS).map { (it as? JsonObject)?.let(SuggestionJson::parse) }
        return GeneratedReview(
            summary = summary,
            suggestions = parsed.filterNotNull(),
            dropped = parsed.count { it == null } + (raw.size - parsed.size).coerceAtLeast(0),
        )
    }
}
