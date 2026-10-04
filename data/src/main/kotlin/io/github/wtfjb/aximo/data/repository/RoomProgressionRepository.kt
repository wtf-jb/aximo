package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.db.dao.ProgressionDao
import io.github.wtfjb.aximo.data.db.entity.ProgressionStateEntity
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.repository.ProgressionRepository

class RoomProgressionRepository(private val dao: ProgressionDao) : ProgressionRepository {

    override suspend fun get(exerciseId: Long): ProgressionState? = dao.get(exerciseId)?.let {
        ProgressionState(it.exerciseId, it.nextWeightKg, it.nextRepTarget, it.reason)
    }

    override suspend fun save(state: ProgressionState) = dao.upsert(
        ProgressionStateEntity(state.exerciseId, state.nextWeightKg, state.nextRepTarget, state.reason),
    )
}
