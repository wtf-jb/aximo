package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.db.dao.RoutineDao
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRoutineRepository(private val dao: RoutineDao) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeRoutinesWithExercises(): Flow<List<RoutineWithExercises>> =
        dao.observeAllWithExercises().map { rows ->
            rows.map { row ->
                RoutineWithExercises(
                    routine = row.routine.toDomain(),
                    exercises = row.exercises.sortedBy { it.position }.map { it.toDomain() },
                )
            }
        }

    override suspend fun getRoutine(id: Long): RoutineWithExercises? {
        val routine = dao.getById(id) ?: return null
        return RoutineWithExercises(
            routine = routine.toDomain(),
            exercises = dao.getExercises(id).map { it.toDomain() },
        )
    }

    override suspend fun saveRoutine(routine: Routine, exercises: List<RoutineExercise>): Long =
        dao.save(routine.toEntity(), exercises.map { it.toEntity() })

    override suspend fun deleteRoutine(id: Long) = dao.delete(id)
}
