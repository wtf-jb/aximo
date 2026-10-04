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
    private val exerciseById: (Long) -> Exercise,
    private val history: Map<Long, List<SetEntry>> = emptyMap(),
) : WorkoutRepository {
    constructor(exercises: Map<Long, Exercise>, history: Map<Long, List<SetEntry>> = emptyMap()) :
        this({ id -> exercises.getValue(id) }, history)

    private val active = MutableStateFlow<WorkoutDetail?>(null)
    private var nextId = 1L

    var finishedNote: String? = null
    val current: WorkoutDetail? get() = active.value

    /** Last finished workout per routine; tests set it directly. */
    val lastPerRoutine = MutableStateFlow<Map<Long, Instant>>(emptyMap())

    override fun observeLastWorkoutPerRoutine(): Flow<Map<Long, Instant>> = lastPerRoutine

    override fun observeActiveWorkout(): Flow<WorkoutDetail?> = active

    /** Finished workouts for "Zuletzt"; tests set it directly. */
    val recentFinished = MutableStateFlow<List<WorkoutDetail>>(emptyList())

    override fun observeRecentFinished(limit: Int): Flow<List<WorkoutDetail>> = recentFinished

    /** All finished workouts for the statistics; tests set it directly. */
    val allFinished = MutableStateFlow<List<WorkoutDetail>>(emptyList())

    override fun observeFinished(): Flow<List<WorkoutDetail>> = allFinished

    override suspend fun previousOfRoutine(routineId: Long, before: Instant): WorkoutDetail? =
        (finished.values + allFinished.value)
            .filter { it.workout.routineId == routineId && it.workout.startedAt < before }
            .maxByOrNull { it.workout.startedAt }

    override suspend fun startWorkout(startedAt: Instant, routineId: Long?): Long {
        val id = nextId++
        active.value = WorkoutDetail(Workout(id = id, startedAt = startedAt, routineId = routineId), emptyList())
        return id
    }

    override suspend fun addExercise(workoutId: Long, exerciseId: Long, supersetGroup: String?, sets: List<PlannedSet>): Long {
        val weId = nextId++
        val detail = WorkoutExerciseDetail(
            entry = WorkoutExercise(weId, workoutId, exerciseId, active.value!!.exercises.size, supersetGroup),
            exercise = exerciseById(exerciseId),
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

    /** Finished workouts by id. */
    val finished = mutableMapOf<Long, WorkoutDetail>()

    override suspend fun finishWorkout(workoutId: Long, endedAt: Instant, note: String) {
        finishedNote = note
        active.value?.let { finished[workoutId] = it.copy(workout = it.workout.copy(endedAt = endedAt, note = note)) }
        active.value = null
    }

    override suspend fun getWorkout(workoutId: Long): WorkoutDetail? =
        active.value?.takeIf { it.workout.id == workoutId } ?: finished[workoutId]
            ?: allFinished.value.find { it.workout.id == workoutId }

    override suspend fun rateWorkout(workoutId: Long, rating: Int?, note: String) {
        finished[workoutId]?.let { finished[workoutId] = it.copy(workout = it.workout.copy(rating = rating, note = note)) }
    }

    override suspend fun setsBefore(exerciseId: Long, before: Instant): List<SetEntry> = history[exerciseId].orEmpty()

    override suspend fun discardWorkout(workoutId: Long) {
        if (active.value?.workout?.id == workoutId) active.value = null
        finished.remove(workoutId)
        allFinished.value = allFinished.value.filter { it.workout.id != workoutId }
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
        rpe = rpe,
        setType = setType,
        completedAt = completedAt,
    )

    private fun edit(change: (WorkoutDetail) -> WorkoutDetail) {
        active.value = active.value?.let(change)
    }

    private fun editExercise(id: Long, change: (WorkoutExerciseDetail) -> WorkoutExerciseDetail) =
        edit { w -> w.copy(exercises = w.exercises.map { if (it.entry.id == id) change(it) else it }) }
}
