package io.github.wtfjb.aximo.domain.repository

import io.github.wtfjb.aximo.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

/** Access to exercises. Implemented in :data. */
interface ExerciseRepository {
    /** All exercises sorted by name; archived ones only if [includeArchived]. */
    fun observeExercises(includeArchived: Boolean = false): Flow<List<Exercise>>

    suspend fun getExercise(id: Long): Exercise?

    /** Inserts (id = 0) or updates the exercise including its muscle groups. Returns the id. */
    suspend fun saveExercise(exercise: Exercise): Long

    suspend fun setArchived(id: Long, archived: Boolean)
}
