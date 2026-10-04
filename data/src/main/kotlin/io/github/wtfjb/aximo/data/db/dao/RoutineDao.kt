package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.wtfjb.aximo.data.db.entity.RoutineEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineWithExerciseRows
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY position, name COLLATE NOCASE")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Transaction
    @Query("SELECT * FROM routines ORDER BY position, name COLLATE NOCASE")
    fun observeAllWithExercises(): Flow<List<RoutineWithExerciseRows>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position")
    suspend fun getExercises(routineId: Long): List<RoutineExerciseEntity>

    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertExercises(exercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercises(routineId: Long)

    /** Saves the routine and replaces its exercises, all or nothing. */
    @Transaction
    suspend fun save(routine: RoutineEntity, exercises: List<RoutineExerciseEntity>): Long {
        val id = if (routine.id == 0L) {
            insert(routine)
        } else {
            update(routine)
            routine.id
        }
        deleteExercises(id)
        insertExercises(exercises.map { it.copy(id = 0, routineId = id) })
        return id
    }
}
