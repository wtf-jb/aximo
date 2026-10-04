package io.github.wtfjb.aximo.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup

@Entity(tableName = "exercises", indices = [Index("name")])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    val note: String,
    val incrementKg: Double,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val restSeconds: Int,
    val roundingStepKg: Double?,
    val catalogId: String?,
    val archived: Boolean,
)

enum class MuscleRole { PRIMARY, SECONDARY }

@Entity(
    tableName = "exercise_muscles",
    primaryKeys = ["exerciseId", "muscleGroup"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseMuscleEntity(
    val exerciseId: Long,
    val muscleGroup: MuscleGroup,
    val role: MuscleRole,
)

/** An exercise row together with its muscle rows, loaded by Room in one go. */
data class ExerciseWithMuscles(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val muscles: List<ExerciseMuscleEntity>,
)
