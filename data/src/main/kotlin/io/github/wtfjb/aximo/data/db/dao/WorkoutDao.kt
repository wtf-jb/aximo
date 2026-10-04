package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.wtfjb.aximo.data.db.entity.SetEntryEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutWithDetails
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    /** The running workout. If there should ever be several, the newest wins. */
    @Transaction
    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutWithDetails?>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntity?

    @Insert
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Query("UPDATE workouts SET endedAt = :endedAt, note = :note WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Instant, note: String)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getExercises(workoutId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun nextExercisePosition(workoutId: Long): Int

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query("UPDATE workout_exercises SET note = :note WHERE id = :id")
    suspend fun setExerciseNote(id: Long, note: String)

    @Insert
    suspend fun insertSet(set: SetEntryEntity): Long

    @Update
    suspend fun updateSet(set: SetEntryEntity)

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM set_entries WHERE workoutExerciseId = :workoutExerciseId ORDER BY position")
    suspend fun getSets(workoutExerciseId: Long): List<SetEntryEntity>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM set_entries WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun nextSetPosition(workoutExerciseId: Long): Int

    /** All sets of [exerciseId] in its most recent finished workout other than [excludeWorkoutId]. */
    @Query(
        """
        SELECT s.* FROM set_entries s
        JOIN workout_exercises we ON s.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId AND we.workoutId = (
            SELECT w.id FROM workouts w
            JOIN workout_exercises we2 ON we2.workoutId = w.id
            WHERE we2.exerciseId = :exerciseId AND w.id != :excludeWorkoutId AND w.endedAt IS NOT NULL
            ORDER BY w.startedAt DESC LIMIT 1
        )
        ORDER BY we.position, s.position
        """,
    )
    suspend fun lastSessionSets(exerciseId: Long, excludeWorkoutId: Long): List<SetEntryEntity>
}
