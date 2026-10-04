package io.github.wtfjb.aximo.domain.chat

/**
 * Shortens the conversation before it is sent (B-05): every request sends the
 * history again, so only the last messages go out.
 */
object ChatHistory {
    /** History sent before the new question: the last 12 messages = 6 questions with their answers. */
    const val MAX_MESSAGES = 12

    /** Rough cap on the text sent as history (about 3,000 tokens). */
    const val MAX_CHARS = 12_000

    /** A question longer than this is cut; the input field has the same limit. */
    const val MAX_QUESTION_CHARS = 2_000

    /** Approximate size of one suggestion in the sent JSON, besides its rationale. */
    private const val SUGGESTION_CHARS = 120

    /**
     * The newest messages that fit in [maxMessages] and [maxChars], oldest first.
     * The result starts with a question: an answer without its question is dropped.
     */
    fun trim(messages: List<ChatMessage>, maxMessages: Int = MAX_MESSAGES, maxChars: Int = MAX_CHARS): List<ChatMessage> {
        val kept = ArrayDeque<ChatMessage>()
        var chars = 0
        for (message in messages.asReversed()) {
            val size = size(message)
            if (kept.size >= maxMessages || chars + size > maxChars) break
            kept.addFirst(message)
            chars += size
        }
        while (kept.isNotEmpty() && kept.first().role != ChatRole.USER) kept.removeFirst()
        return kept.toList()
    }

    fun size(message: ChatMessage): Int =
        message.text.length + message.suggestions.sumOf { it.rationale.length + SUGGESTION_CHARS }
}
