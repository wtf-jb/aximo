package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.wtfjb.aximo.data.db.entity.SetEntryEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

/** Basic access to workouts. The logging use cases (A-02) build on this. */
@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntity?

    @Insert
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getExercises(workoutId: Long): List<WorkoutExerciseEntity>

    @Insert
    suspend fun insertSet(set: SetEntryEntity): Long

    @Update
    suspend fun updateSet(set: SetEntryEntity)

    @Query("SELECT * FROM set_entries WHERE workoutExerciseId = :workoutExerciseId ORDER BY position")
    suspend fun getSets(workoutExerciseId: Long): List<SetEntryEntity>
}
