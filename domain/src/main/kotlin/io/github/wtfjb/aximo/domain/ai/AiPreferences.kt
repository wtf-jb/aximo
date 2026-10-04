package io.github.wtfjb.aximo.domain.ai

import io.github.wtfjb.aximo.domain.review.WeeklyReviewSetting
import kotlinx.coroutines.flow.Flow

/** Settings of the AI features (Prio B). Implemented in :data. */
interface AiPreferences {
    /** The user has seen what is sent and agreed (requirements: notice before the first AI call). */
    val dataNoticeAccepted: Flow<Boolean>

    suspend fun acceptDataNotice()

    /**
     * The user has seen the chat's own notice (B-05): the chat also sends free
     * text and the conversation, again with every message.
     */
    val chatNoticeAccepted: Flow<Boolean>

    /** Accepts the chat notice; implies [acceptDataNotice]. */
    suspend fun acceptChatNotice()

    /** Automatic weekly review (B-02). */
    val weeklyReview: Flow<WeeklyReviewSetting>

    suspend fun setWeeklyReview(setting: WeeklyReviewSetting)
}
