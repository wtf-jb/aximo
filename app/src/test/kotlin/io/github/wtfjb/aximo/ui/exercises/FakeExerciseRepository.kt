package io.github.wtfjb.aximo.ui.exercises

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository for ViewModel tests. */
class FakeExerciseRepository(initial: List<Exercise> = emptyList()) : ExerciseRepository {
    private val exercises = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val all: List<Exercise> get() = exercises.value

    override fun observeExercises(includeArchived: Boolean): Flow<List<Exercise>> =
        exercises.map { list -> list.filter { includeArchived || !it.archived }.sortedBy { it.name.lowercase() } }

    override suspend fun getExercise(id: Long): Exercise? = exercises.value.find { it.id == id }

    override suspend fun saveExercise(exercise: Exercise): Long {
        val id = if (exercise.id == 0L) nextId++ else exercise.id
        exercises.value = exercises.value.filter { it.id != id } + exercise.copy(id = id)
        return id
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        exercises.value = exercises.value.map { if (it.id == id) it.copy(archived = archived) else it }
    }
}
