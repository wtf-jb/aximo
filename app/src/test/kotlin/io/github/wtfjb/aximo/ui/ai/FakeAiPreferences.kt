package io.github.wtfjb.aximo.ui.ai

import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.review.WeeklyReviewSetting
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAiPreferences : AiPreferences {
    val accepted = MutableStateFlow(false)
    override val dataNoticeAccepted = accepted

    override suspend fun acceptDataNotice() {
        accepted.value = true
    }

    override val weeklyReview = MutableStateFlow(WeeklyReviewSetting())

    override suspend fun setWeeklyReview(setting: WeeklyReviewSetting) {
        weeklyReview.value = setting
    }
}
