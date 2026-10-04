package io.github.wtfjb.aximo.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val weeks: Int,
)

@Entity(
    tableName = "blocks",
    foreignKeys = [
        ForeignKey(
            entity = CycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cycleId")],
)
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val name: String,
    val position: Int,
    val weeks: Int,
    val isDeload: Boolean,
)

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = CycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("cycleId")],
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
    val cycleId: Long?,
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        // An exercise that is used in a routine can't be deleted, only archived.
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int?,
    val supersetGroup: String?,
)

/** A routine with its exercise rows, loaded by Room in one go. */
data class RoutineWithExerciseRows(
    @androidx.room.Embedded val routine: RoutineEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseEntity>,
)
