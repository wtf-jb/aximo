package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.wtfjb.aximo.data.db.entity.BlockEntity
import io.github.wtfjb.aximo.data.db.entity.CardioEntryEntity
import io.github.wtfjb.aximo.data.db.entity.CycleEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseMuscleEntity
import io.github.wtfjb.aximo.data.db.entity.ProgressionStateEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.SetEntryEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutExerciseEntity

/** Reads and writes whole tables, for export and restore (A-08). */
@Dao
interface BackupDao {
    @Query("SELECT * FROM exercises ORDER BY id") suspend fun exercises(): List<ExerciseEntity>
    @Query("SELECT * FROM exercise_muscles ORDER BY exerciseId, muscleGroup") suspend fun exerciseMuscles(): List<ExerciseMuscleEntity>
    @Query("SELECT * FROM cycles ORDER BY id") suspend fun cycles(): List<CycleEntity>
    @Query("SELECT * FROM blocks ORDER BY id") suspend fun blocks(): List<BlockEntity>
    @Query("SELECT * FROM routines ORDER BY id") suspend fun routines(): List<RoutineEntity>
    @Query("SELECT * FROM routine_exercises ORDER BY id") suspend fun routineExercises(): List<RoutineExerciseEntity>
    @Query("SELECT * FROM workouts ORDER BY id") suspend fun workouts(): List<WorkoutEntity>
    @Query("SELECT * FROM workout_exercises ORDER BY id") suspend fun workoutExercises(): List<WorkoutExerciseEntity>
    @Query("SELECT * FROM set_entries ORDER BY id") suspend fun sets(): List<SetEntryEntity>
    @Query("SELECT * FROM cardio_entries ORDER BY id") suspend fun cardioEntries(): List<CardioEntryEntity>
    @Query("SELECT * FROM progression_states ORDER BY exerciseId") suspend fun progression(): List<ProgressionStateEntity>

    @Insert suspend fun insertExercises(rows: List<ExerciseEntity>)
    @Insert suspend fun insertExerciseMuscles(rows: List<ExerciseMuscleEntity>)
    @Insert suspend fun insertCycles(rows: List<CycleEntity>)
    @Insert suspend fun insertBlocks(rows: List<BlockEntity>)
    @Insert suspend fun insertRoutines(rows: List<RoutineEntity>)
    @Insert suspend fun insertRoutineExercises(rows: List<RoutineExerciseEntity>)
    @Insert suspend fun insertWorkouts(rows: List<WorkoutEntity>)
    @Insert suspend fun insertWorkoutExercises(rows: List<WorkoutExerciseEntity>)
    @Insert suspend fun insertSets(rows: List<SetEntryEntity>)
    @Insert suspend fun insertCardioEntries(rows: List<CardioEntryEntity>)
    @Insert suspend fun insertProgression(rows: List<ProgressionStateEntity>)

    // Children first, so foreign keys never point to a missing row.
    @Query("DELETE FROM progression_states") suspend fun deleteProgression()
    @Query("DELETE FROM cardio_entries") suspend fun deleteCardioEntries()
    @Query("DELETE FROM set_entries") suspend fun deleteSets()
    @Query("DELETE FROM workout_exercises") suspend fun deleteWorkoutExercises()
    @Query("DELETE FROM workouts") suspend fun deleteWorkouts()
    @Query("DELETE FROM routine_exercises") suspend fun deleteRoutineExercises()
    @Query("DELETE FROM routines") suspend fun deleteRoutines()
    @Query("DELETE FROM blocks") suspend fun deleteBlocks()
    @Query("DELETE FROM cycles") suspend fun deleteCycles()
    @Query("DELETE FROM exercise_muscles") suspend fun deleteExerciseMuscles()
    @Query("DELETE FROM exercises") suspend fun deleteExercises()

    suspend fun deleteAll() {
        deleteProgression()
        deleteCardioEntries()
        deleteSets()
        deleteWorkoutExercises()
        deleteWorkouts()
        deleteRoutineExercises()
        deleteRoutines()
        deleteBlocks()
        deleteCycles()
        deleteExerciseMuscles()
        deleteExercises()
    }
}
