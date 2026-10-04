package io.github.wtfjb.aximo.domain.repository

import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

/** Access to workouts and their sets. Implemented in :data. */
interface WorkoutRepository {
    /** Start time of the last finished workout per routine id. */
    fun observeLastWorkoutPerRoutine(): Flow<Map<Long, Instant>>

    /** The running workout (no end time), or null. There is at most one. */
    fun observeActiveWorkout(): Flow<WorkoutDetail?>

    suspend fun startWorkout(startedAt: Instant, routineId: Long? = null): Long

    /** Adds the exercise at the end of the workout with the given sets. Returns the workout-exercise id. */
    suspend fun addExercise(workoutId: Long, exerciseId: Long, supersetGroup: String?, sets: List<PlannedSet>): Long

    suspend fun removeExercise(workoutExerciseId: Long)

    suspend fun setExerciseNote(workoutExerciseId: Long, note: String)

    /** Adds a set at the end of the exercise. Returns the set id. */
    suspend fun addSet(workoutExerciseId: Long, set: PlannedSet): Long

    suspend fun updateSet(set: SetEntry)

    suspend fun deleteSet(setId: Long)

    suspend fun finishWorkout(workoutId: Long, endedAt: Instant, note: String)

    /** Deletes the workout with all its sets. */
    suspend fun discardWorkout(workoutId: Long)

    /**
     * All sets of the exercise from its most recent finished workout other than
     * [excludeWorkoutId], ordered as logged. Empty if there is none.
     */
    suspend fun lastSessionSets(exerciseId: Long, excludeWorkoutId: Long): List<SetEntry>
}
