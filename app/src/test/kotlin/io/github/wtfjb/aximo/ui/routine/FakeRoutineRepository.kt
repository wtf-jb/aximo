package io.github.wtfjb.aximo.ui.routine

import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory routine repository for ViewModel tests. */
class FakeRoutineRepository(initial: List<RoutineWithExercises> = emptyList()) : RoutineRepository {
    private val data = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.routine.id } ?: 0) + 1

    val all: List<RoutineWithExercises> get() = data.value

    override fun observeRoutines(): Flow<List<Routine>> = observeRoutinesWithExercises().map { list -> list.map { it.routine } }

    override fun observeRoutinesWithExercises(): Flow<List<RoutineWithExercises>> =
        data.map { list -> list.sortedBy { it.routine.position } }

    override suspend fun getRoutine(id: Long): RoutineWithExercises? = data.value.find { it.routine.id == id }

    override suspend fun saveRoutine(routine: Routine, exercises: List<RoutineExercise>): Long {
        val id = if (routine.id == 0L) nextId++ else routine.id
        val saved = RoutineWithExercises(routine.copy(id = id), exercises.map { it.copy(routineId = id) })
        data.value = data.value.filter { it.routine.id != id } + saved
        return id
    }

    override suspend fun deleteRoutine(id: Long) {
        data.value = data.value.filter { it.routine.id != id }
    }
}
