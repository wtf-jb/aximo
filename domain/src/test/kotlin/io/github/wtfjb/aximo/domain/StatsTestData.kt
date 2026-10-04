package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** Shared builders for the statistics tests. All times in UTC. */
object StatsTestData {
    val zone = TimeZone.UTC

    val bench = Exercise(
        id = 1,
        name = "Bankdrücken",
        type = ExerciseType.STRENGTH,
        equipment = Equipment.BARBELL,
        primaryMuscles = setOf(MuscleGroup.CHEST),
        secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
    )
    val pullUp = Exercise(
        id = 2,
        name = "Klimmzug",
        type = ExerciseType.BODYWEIGHT,
        equipment = Equipment.BODYWEIGHT,
        primaryMuscles = setOf(MuscleGroup.LATS, MuscleGroup.UPPER_BACK),
        secondaryMuscles = setOf(MuscleGroup.BICEPS),
    )

    /** 10:00 UTC on [date]. */
    fun at(date: LocalDate): Instant = date.atStartOfDayIn(zone) + 10.hours

    fun set(weight: Double, reps: Int, type: SetType = SetType.WORKING, done: Boolean = true) =
        SetEntry(workoutExerciseId = 0, position = 0, weightKg = weight, reps = reps, setType = type, completedAt = if (done) Instant.fromEpochSeconds(0) else null)

    fun workout(
        id: Long,
        start: Instant,
        vararg exercises: Pair<Exercise, List<SetEntry>>,
        routineId: Long? = null,
        finished: Boolean = true,
    ) = WorkoutDetail(
        workout = Workout(id = id, startedAt = start, endedAt = if (finished) start + 1.hours else null, routineId = routineId),
        exercises = exercises.mapIndexed { index, (exercise, sets) ->
            WorkoutExerciseDetail(WorkoutExercise(id * 100 + index, id, exercise.id, index), exercise, sets)
        },
    )
}
