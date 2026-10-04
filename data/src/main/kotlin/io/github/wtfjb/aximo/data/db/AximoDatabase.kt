package io.github.wtfjb.aximo.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.wtfjb.aximo.data.db.dao.AiProfileDao
import io.github.wtfjb.aximo.data.db.dao.AiReviewDao
import io.github.wtfjb.aximo.data.db.dao.BackupDao
import io.github.wtfjb.aximo.data.db.dao.CardioDao
import io.github.wtfjb.aximo.data.db.dao.ExerciseDao
import io.github.wtfjb.aximo.data.db.dao.ProgressionDao
import io.github.wtfjb.aximo.data.db.dao.RoutineDao
import io.github.wtfjb.aximo.data.db.dao.WorkoutDao
import io.github.wtfjb.aximo.data.db.entity.AiProfileEntity
import io.github.wtfjb.aximo.data.db.entity.AiReviewEntity
import io.github.wtfjb.aximo.data.db.entity.AiSuggestionEntity
import io.github.wtfjb.aximo.data.db.entity.BlockEntity
import io.github.wtfjb.aximo.data.db.entity.CardioEntryEntity
import io.github.wtfjb.aximo.data.db.entity.CycleEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.ExerciseMuscleEntity
import io.github.wtfjb.aximo.data.db.entity.ProgressionStateEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineEntity
import io.github.wtfjb.aximo.data.db.entity.RoutineExerciseEntity
import io.github.wtfjb.aximo.data.db.entity.SetEntryEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutEntity
import io.github.wtfjb.aximo.data.db.entity.WorkoutExerciseEntity

/**
 * The local database. Every schema change raises [version] and needs a
 * migration; the schemas are exported to data/schemas.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        CycleEntity::class,
        BlockEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SetEntryEntity::class,
        CardioEntryEntity::class,
        ProgressionStateEntity::class,
        AiProfileEntity::class,
        AiReviewEntity::class,
        AiSuggestionEntity::class,
    ],
    // 2: ai_profiles (B-01); 3: ai_reviews, ai_suggestions (B-02).
    // New tables only, so Room migrates automatically.
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AximoDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun cardioDao(): CardioDao
    abstract fun progressionDao(): ProgressionDao
    abstract fun backupDao(): BackupDao
    abstract fun aiProfileDao(): AiProfileDao
    abstract fun aiReviewDao(): AiReviewDao

    companion object {
        private const val FILE_NAME = "aximo.db"

        fun build(context: Context): AximoDatabase =
            Room.databaseBuilder(context, AximoDatabase::class.java, FILE_NAME).build()
    }
}
