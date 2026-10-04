package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.logging.LoggingGenerator
import io.github.wtfjb.aximo.domain.logging.LoggingInput
import io.github.wtfjb.aximo.domain.logging.ParsedLog

/** Logging by text over any provider: [LoggingPrompt] out, [LoggingParser] in. */
class KtorLoggingGenerator : LoggingGenerator {
    override suspend fun parse(provider: AiProvider, input: LoggingInput, language: String): ParsedLog =
        LoggingParser.parse(provider.complete(LoggingPrompt.request(input, language)))
}
