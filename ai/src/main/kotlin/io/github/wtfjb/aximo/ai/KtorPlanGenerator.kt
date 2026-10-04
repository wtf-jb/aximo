package io.github.wtfjb.aximo.ai

import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.plan.GeneratedPlan
import io.github.wtfjb.aximo.domain.plan.PlanGenerator
import io.github.wtfjb.aximo.domain.plan.PlanInput

/** Plan generation over any provider: [PlanPrompt] out, [PlanParser] in. */
class KtorPlanGenerator : PlanGenerator {
    override suspend fun generate(provider: AiProvider, input: PlanInput, language: String): GeneratedPlan =
        PlanParser.parse(provider.complete(PlanPrompt.request(input, language)))
}
