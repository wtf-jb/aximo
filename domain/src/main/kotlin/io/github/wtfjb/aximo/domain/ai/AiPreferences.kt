package io.github.wtfjb.aximo.domain.ai

import kotlinx.coroutines.flow.Flow

/** Settings of the AI features (Prio B). Implemented in :data. */
interface AiPreferences {
    /** The user has seen what is sent and agreed (requirements: notice before the first AI call). */
    val dataNoticeAccepted: Flow<Boolean>

    suspend fun acceptDataNotice()
}
