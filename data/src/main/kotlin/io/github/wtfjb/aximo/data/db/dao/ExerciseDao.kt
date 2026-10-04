package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.wtfjb.aximo.data.db.entity.ExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseMuscleEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseWithMuscles
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Transaction
    @Query(
        "SELECT * FROM exercises WHERE archived = 0 OR :includeArchived " +
            "ORDER BY name COLLATE NOCASE",
    )
    fun observeAll(includeArchived: Boolean): Flow<List<ExerciseWithMuscles>>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseWithMuscles?

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Insert
    suspend fun insertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Query("DELETE FROM exercise_muscles WHERE exerciseId = :exerciseId")
    suspend fun deleteMuscles(exerciseId: Long)

    /** Saves the exercise and replaces its muscle groups, all or nothing. */
    @Transaction
    suspend fun save(exercise: ExerciseEntity, muscles: List<ExerciseMuscleEntity>): Long {
        val id = if (exercise.id == 0L) {
            insert(exercise)
        } else {
            update(exercise)
            exercise.id
        }
        deleteMuscles(id)
        insertMuscles(muscles.map { it.copy(exerciseId = id) })
        return id
    }
}
