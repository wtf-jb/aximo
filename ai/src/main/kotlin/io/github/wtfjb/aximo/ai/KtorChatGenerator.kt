package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.chat.ChatGenerator
import io.github.wtfjb.aximo.domain.chat.ChatPayload
import io.github.wtfjb.aximo.domain.chat.GeneratedReply

/** Coach chat over any provider: [ChatPrompt] out, [ChatParser] in. */
class KtorChatGenerator : ChatGenerator {
    override suspend fun generate(provider: AiProvider, payload: ChatPayload, language: String): GeneratedReply =
        ChatParser.parse(provider.complete(ChatPrompt.request(payload, language)))
}
