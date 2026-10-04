package io.github.wtfjb.aximo.ui.workout

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory workout repository; knows the exercises so it can build details. */
class FakeWorkoutRepository(
    private val exercises: Map<Long, Exercise>,
    private val history: Map<Long, List<SetEntry>> = emptyMap(),
) : WorkoutRepository {
    private val active = MutableStateFlow<WorkoutDetail?>(null)
    private var nextId = 1L

    var finishedNote: String? = null
    val current: WorkoutDetail? get() = active.value

    override fun observeActiveWorkout(): Flow<WorkoutDetail?> = active

    override suspend fun startWorkout(startedAt: Instant, routineId: Long?): Long {
        val id = nextId++
        active.value = WorkoutDetail(Workout(id = id, startedAt = startedAt, routineId = routineId), emptyList())
        return id
    }

    override suspend fun addExercise(workoutId: Long, exerciseId: Long, supersetGroup: String?, sets: List<PlannedSet>): Long {
        val weId = nextId++
        val detail = WorkoutExerciseDetail(
            entry = WorkoutExercise(weId, workoutId, exerciseId, active.value!!.exercises.size, supersetGroup),
            exercise = exercises.getValue(exerciseId),
            sets = sets.mapIndexed { i, p -> p.toEntry(weId, i) },
        )
        edit { it.copy(exercises = it.exercises + detail) }
        return weId
    }

    override suspend fun removeExercise(workoutExerciseId: Long) =
        edit { w -> w.copy(exercises = w.exercises.filter { it.entry.id != workoutExerciseId }) }

    override suspend fun setExerciseNote(workoutExerciseId: Long, note: String) =
        editExercise(workoutExerciseId) { it.copy(entry = it.entry.copy(note = note)) }

    override suspend fun addSet(workoutExerciseId: Long, set: PlannedSet): Long {
        val id = nextId++
        editExercise(workoutExerciseId) { it.copy(sets = it.sets + set.toEntry(workoutExerciseId, it.sets.size).copy(id = id)) }
        return id
    }

    override suspend fun updateSet(set: SetEntry) =
        editExercise(set.workoutExerciseId) { d -> d.copy(sets = d.sets.map { if (it.id == set.id) set else it }) }

    override suspend fun deleteSet(setId: Long) =
        edit { w -> w.copy(exercises = w.exercises.map { d -> d.copy(sets = d.sets.filter { it.id != setId }) }) }

    override suspend fun finishWorkout(workoutId: Long, endedAt: Instant, note: String) {
        finishedNote = note
        active.value = null
    }

    override suspend fun discardWorkout(workoutId: Long) {
        active.value = null
    }

    override suspend fun lastSessionSets(exerciseId: Long, excludeWorkoutId: Long): List<SetEntry> =
        history[exerciseId].orEmpty()

    private fun PlannedSet.toEntry(weId: Long, position: Int) = SetEntry(
        id = nextId++,
        workoutExerciseId = weId,
        position = position,
        weightKg = weightKg,
        reps = reps,
        rir = rir,
        setType = setType,
    )

    private fun edit(change: (WorkoutDetail) -> WorkoutDetail) {
        active.value = active.value?.let(change)
    }

    private fun editExercise(id: Long, change: (WorkoutExerciseDetail) -> WorkoutExerciseDetail) =
        edit { w -> w.copy(exercises = w.exercises.map { if (it.entry.id == id) change(it) else it }) }
}
