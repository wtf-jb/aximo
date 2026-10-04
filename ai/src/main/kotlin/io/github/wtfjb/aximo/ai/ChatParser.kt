package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.ai.JsonRead.string
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.chat.GeneratedReply
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Validates the chat answer against the schema in [ChatPrompt] (B-05). The
 * reply is required; a suggestion that does not match is dropped and counted.
 * Whether ids and `from` values fit the routines is checked later in the domain.
 */
object ChatParser {
    /** Longer replies are cut; the prompt asks for a few sentences. */
    const val MAX_REPLY_CHARS = 4_000

    fun parse(text: String): GeneratedReply {
        val root = JsonRead.extractObject(text)
        if (root == null) {
            // Small local models sometimes ignore the format and answer in plain
            // text. That is still an answer, just without suggestions.
            val plain = text.trim()
            if (plain.isEmpty() || plain.contains('{')) throw AiException(AiException.Reason.INVALID_RESPONSE)
            return GeneratedReply(plain.take(MAX_REPLY_CHARS))
        }
        val reply = root.string("reply")?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw AiException(AiException.Reason.INVALID_RESPONSE)
        val raw = (root["suggestions"] as? JsonArray).orEmpty()
        val parsed = raw.take(ChatPrompt.MAX_SUGGESTIONS).map { (it as? JsonObject)?.let(SuggestionJson::parse) }
        return GeneratedReply(
            reply = reply.take(MAX_REPLY_CHARS),
            suggestions = parsed.filterNotNull(),
            dropped = parsed.count { it == null } + (raw.size - parsed.size),
        )
    }
}
