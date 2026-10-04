package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.db.entity.ExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseMuscleEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseWithMuscles
import io.github.wtfjb.aximo.data.db.entity.MuscleRole
import io.github.wtfjb.aximo.data.db.entity.RoutineEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.SetEntryEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutWithDetails
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail

// Conversion between database rows (:data) and domain models (:domain).

internal fun ExerciseWithMuscles.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    type = exercise.type,
    equipment = exercise.equipment,
    primaryMuscles = muscles.filter { it.role == MuscleRole.PRIMARY }.map { it.muscleGroup }.toSet(),
    secondaryMuscles = muscles.filter { it.role == MuscleRole.SECONDARY }.map { it.muscleGroup }.toSet(),
    note = exercise.note,
    incrementKg = exercise.incrementKg,
    repRangeMin = exercise.repRangeMin,
    repRangeMax = exercise.repRangeMax,
    restSeconds = exercise.restSeconds,
    roundingStepKg = exercise.roundingStepKg,
    catalogId = exercise.catalogId,
    archived = exercise.archived,
)

internal fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name.trim(),
    type = type,
    equipment = equipment,
    note = note,
    incrementKg = incrementKg,
    repRangeMin = repRangeMin,
    repRangeMax = repRangeMax,
    restSeconds = restSeconds,
    roundingStepKg = roundingStepKg,
    catalogId = catalogId,
    archived = archived,
)

internal fun Exercise.toMuscleEntities(): List<ExerciseMuscleEntity> =
    primaryMuscles.map { ExerciseMuscleEntity(id, it, MuscleRole.PRIMARY) } +
        secondaryMuscles.map { ExerciseMuscleEntity(id, it, MuscleRole.SECONDARY) }

internal fun RoutineEntity.toDomain() = Routine(id = id, name = name, position = position, cycleId = cycleId)

internal fun Routine.toEntity() = RoutineEntity(id = id, name = name.trim(), position = position, cycleId = cycleId)

internal fun RoutineExerciseEntity.toDomain() = RoutineExercise(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    position = position,
    targetSets = targetSets,
    repMin = repMin,
    repMax = repMax,
    targetRir = targetRir,
    supersetGroup = supersetGroup,
)

internal fun RoutineExercise.toEntity() = RoutineExerciseEntity(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    position = position,
    targetSets = targetSets,
    repMin = repMin,
    repMax = repMax,
    targetRir = targetRir,
    supersetGroup = supersetGroup,
)

internal fun WorkoutEntity.toDomain() = Workout(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    routineId = routineId,
    note = note,
    rating = rating,
)

internal fun WorkoutExerciseEntity.toDomain() = WorkoutExercise(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    supersetGroup = supersetGroup,
    note = note,
)

internal fun SetEntryEntity.toDomain() = SetEntry(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    rir = rir,
    setType = setType,
    completedAt = completedAt,
)

internal fun SetEntry.toEntity() = SetEntryEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    rir = rir,
    setType = setType,
    completedAt = completedAt,
)

internal fun WorkoutWithDetails.toDomain() = WorkoutDetail(
    workout = workout.toDomain(),
    exercises = exercises.sortedBy { it.entry.position }.map { row ->
        WorkoutExerciseDetail(
            entry = row.entry.toDomain(),
            exercise = row.exercise.toDomain(),
            sets = row.sets.sortedBy { it.position }.map { it.toDomain() },
        )
    },
)
