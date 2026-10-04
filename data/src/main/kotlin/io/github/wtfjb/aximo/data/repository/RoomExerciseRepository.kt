package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.db.dao.ExerciseDao
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExerciseRepository(private val dao: ExerciseDao) : ExerciseRepository {

    override fun observeExercises(includeArchived: Boolean): Flow<List<Exercise>> =
        dao.observeAll(includeArchived).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getExercise(id: Long): Exercise? = dao.getById(id)?.toDomain()

    override suspend fun saveExercise(exercise: Exercise): Long =
        dao.save(exercise.toEntity(), exercise.toMuscleEntities())

    override suspend fun setArchived(id: Long, archived: Boolean) = dao.setArchived(id, archived)
}
