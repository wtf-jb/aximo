package io.github.wtfjb.aximo.domain.repository

import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

/** Access to routines. Implemented in :data. */
interface RoutineRepository {
    /** All routines ordered by position. */
    fun observeRoutines(): Flow<List<Routine>>

    suspend fun getRoutine(id: Long): RoutineWithExercises?

    /**
     * Inserts (id = 0) or updates the routine and replaces its exercises with
     * [exercises]. Returns the routine id.
     */
    suspend fun saveRoutine(routine: Routine, exercises: List<RoutineExercise>): Long

    suspend fun deleteRoutine(id: Long)
}
