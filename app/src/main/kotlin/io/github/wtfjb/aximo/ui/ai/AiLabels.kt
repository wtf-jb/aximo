package io.github.wtfjb.aximo.ui.ai

import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProviderKind

fun AiProviderKind.label(): Int = when (this) {
    AiProviderKind.OPENAI_COMPATIBLE -> R.string.ai_kind_openai
    AiProviderKind.ANTHROPIC -> R.string.ai_kind_anthropic
}

fun AiException.Reason.label(): Int = when (this) {
    AiException.Reason.UNAUTHORIZED -> R.string.ai_error_unauthorized
    AiException.Reason.NOT_FOUND -> R.string.ai_error_not_found
    AiException.Reason.RATE_LIMITED -> R.string.ai_error_rate_limited
    AiException.Reason.REJECTED -> R.string.ai_error_rejected
    AiException.Reason.SERVER -> R.string.ai_error_server
    AiException.Reason.TIMEOUT -> R.string.ai_error_timeout
    AiException.Reason.NETWORK -> R.string.ai_error_network
    AiException.Reason.INVALID_RESPONSE -> R.string.ai_error_invalid_response
    AiException.Reason.REFUSED -> R.string.ai_error_refused
}
