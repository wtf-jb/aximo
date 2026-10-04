package io.github.wtfjb.aximo.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.wtfjb.aximo.domain.model.CardioSource
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.SetType
import kotlin.time.Instant

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("routineId"), Index("startedAt")],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Instant,
    val endedAt: Instant?,
    val routineId: Long?,
    val note: String,
    val rating: Int?,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val supersetGroup: String?,
    val note: String,
)

@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val rir: Int?,
    val setType: SetType,
    val completedAt: Instant?,
)

@Entity(
    tableName = "cardio_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId"), Index("startedAt"), Index("externalId")],
)
data class CardioEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long?,
    val exerciseId: Long,
    val startedAt: Instant,
    val durationSec: Int,
    val distanceM: Double?,
    val avgHeartRate: Int?,
    val elevationM: Double?,
    val note: String,
    val source: CardioSource,
    val externalId: String?,
)

@Entity(
    tableName = "progression_states",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProgressionStateEntity(
    @PrimaryKey val exerciseId: Long,
    val nextWeightKg: Double,
    val nextRepTarget: Int,
    val reason: ProgressionReason,
)

/** A workout exercise with its exercise (incl. muscles) and its sets. */
data class WorkoutExerciseWithDetails(
    @androidx.room.Embedded val entry: WorkoutExerciseEntity,
    @androidx.room.Relation(entity = ExerciseEntity::class, parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseWithMuscles,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<SetEntryEntity>,
)

/** A workout with all its exercises and sets, loaded in one go. */
data class WorkoutWithDetails(
    @androidx.room.Embedded val workout: WorkoutEntity,
    @androidx.room.Relation(entity = WorkoutExerciseEntity::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseWithDetails>,
)

/** Result row: when a routine was last trained. */
data class RoutineLastWorkout(
    val routineId: Long,
    val lastStartedAt: Instant,
)
