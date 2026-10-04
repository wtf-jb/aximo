package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.db.dao.WorkoutDao
import io.github.wtfjb.aximo.data.db.entity.SetEntryEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutExerciseEntity
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkoutRepository(private val dao: WorkoutDao) : WorkoutRepository {

    override fun observeLastWorkoutPerRoutine(): Flow<Map<Long, Instant>> =
        dao.observeLastPerRoutine().map { rows -> rows.associate { it.routineId to it.lastStartedAt } }

    override fun observeActiveWorkout(): Flow<WorkoutDetail?> =
        dao.observeActive().map { it?.toDomain() }

    override fun observeRecentFinished(limit: Int): Flow<List<WorkoutDetail>> =
        dao.observeRecentFinished(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeFinished(): Flow<List<WorkoutDetail>> =
        dao.observeFinished().map { rows -> rows.map { it.toDomain() } }

    override suspend fun previousOfRoutine(routineId: Long, before: Instant): WorkoutDetail? =
        dao.previousOfRoutine(routineId, before)?.toDomain()

    override suspend fun startWorkout(startedAt: Instant, routineId: Long?): Long =
        dao.insert(WorkoutEntity(startedAt = startedAt, endedAt = null, routineId = routineId, note = "", rating = null))

    override suspend fun addExercise(
        workoutId: Long,
        exerciseId: Long,
        supersetGroup: String?,
        sets: List<PlannedSet>,
    ): Long {
        val id = dao.insertExercise(
            WorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = dao.nextExercisePosition(workoutId),
                supersetGroup = supersetGroup,
                note = "",
            ),
        )
        sets.forEachIndexed { index, set -> dao.insertSet(set.toEntity(id, index)) }
        return id
    }

    override suspend fun removeExercise(workoutExerciseId: Long) = dao.deleteExercise(workoutExerciseId)

    override suspend fun setExerciseNote(workoutExerciseId: Long, note: String) =
        dao.setExerciseNote(workoutExerciseId, note.trim())

    override suspend fun addSet(workoutExerciseId: Long, set: PlannedSet): Long =
        dao.insertSet(set.toEntity(workoutExerciseId, dao.nextSetPosition(workoutExerciseId)))

    override suspend fun updateSet(set: SetEntry) = dao.updateSet(set.toEntity())

    override suspend fun deleteSet(setId: Long) = dao.deleteSet(setId)

    override suspend fun finishWorkout(workoutId: Long, endedAt: Instant, note: String) =
        dao.finish(workoutId, endedAt, note.trim())

    override suspend fun discardWorkout(workoutId: Long) = dao.delete(workoutId)

    override suspend fun getWorkout(workoutId: Long): WorkoutDetail? = dao.getWithDetails(workoutId)?.toDomain()

    override suspend fun rateWorkout(workoutId: Long, rating: Int?, note: String) = dao.rate(workoutId, rating, note.trim())

    override suspend fun setsBefore(exerciseId: Long, before: Instant): List<SetEntry> =
        dao.setsBefore(exerciseId, before).map { it.toDomain() }

    override suspend fun lastSessionSets(exerciseId: Long, excludeWorkoutId: Long): List<SetEntry> =
        dao.lastSessionSets(exerciseId, excludeWorkoutId).map { it.toDomain() }

    private fun PlannedSet.toEntity(workoutExerciseId: Long, position: Int) = SetEntryEntity(
        workoutExerciseId = workoutExerciseId,
        position = position,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        rir = rir,
        setType = setType,
        completedAt = completedAt,
    )
}
