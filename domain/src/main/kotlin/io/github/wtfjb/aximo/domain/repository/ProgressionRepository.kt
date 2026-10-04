package io.github.wtfjb.aximo.domain.repository

import io.github.wtfjb.aximo.domain.model.ProgressionState

/** Stored progression suggestions, one per exercise (A-06). Implemented in :data. */
interface ProgressionRepository {
    suspend fun get(exerciseId: Long): ProgressionState?

    suspend fun save(state: ProgressionState)
}
