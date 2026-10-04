package io.github.wtfjb.aximo.ui.workout

import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.repository.ProgressionRepository

/** In-memory progression store for ViewModel tests. */
class FakeProgressionRepository(initial: List<ProgressionState> = emptyList()) : ProgressionRepository {
    val states = initial.associateBy { it.exerciseId }.toMutableMap()

    override suspend fun get(exerciseId: Long): ProgressionState? = states[exerciseId]

    override suspend fun save(state: ProgressionState) {
        states[state.exerciseId] = state
    }
}
