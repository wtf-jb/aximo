package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.review.GeneratedReview
import io.github.wtfjb.aximo.domain.review.ReviewContext
import io.github.wtfjb.aximo.domain.review.ReviewGenerator

/** Weekly review over any provider: [ReviewPrompt] out, [ReviewParser] in. */
class KtorReviewGenerator : ReviewGenerator {
    override suspend fun generate(provider: AiProvider, context: ReviewContext, language: String): GeneratedReview =
        ReviewParser.parse(provider.complete(ReviewPrompt.request(context, language)))
}
