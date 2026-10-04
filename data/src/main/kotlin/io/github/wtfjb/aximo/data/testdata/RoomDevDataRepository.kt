package io.github.wtfjb.aximo.data.testdata

import androidx.room.withTransaction
import io.github.wtfjb.aximo.data.backup.BackupFile
import io.github.wtfjb.aximo.data.backup.RoomBackupRepository
import io.github.wtfjb.aximo.data.db.AximoDatabase
import io.github.wtfjb.aximo.domain.devdata.DevDataRepository
import io.github.wtfjb.aximo.domain.exercise.CatalogSeeder
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json

/**
 * Sample data and wipe for testing. Both start from an empty database with the
 * standard exercises; the sample data then goes through the normal backup restore,
 * so it is checked with the same rules as an imported file.
 */
class RoomDevDataRepository(
    private val db: AximoDatabase,
    private val backup: RoomBackupRepository,
    private val seeder: CatalogSeeder,
    private val time: TimeSource,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val generator: TestDataGenerator = TestDataGenerator(),
) : DevDataRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadTestData() {
        wipe()
        // Right after the wipe the export holds just the standard exercises (and the settings).
        val base = json.decodeFromString(BackupFile.serializer(), backup.exportJson())
        val file = generator.generate(base, time.now(), zone())
        backup.restoreJson(json.encodeToString(BackupFile.serializer(), file))
    }

    override suspend fun clearAll() {
        wipe()
    }

    /** Deletes all user data, then creates the standard exercises again. */
    private suspend fun wipe() {
        db.withTransaction {
            db.backupDao().deleteAll()
            // Suggestions point to reviews and chat messages, so they go first.
            db.aiReviewDao().deleteAllSuggestions()
            db.aiChatDao().deleteMessages()
            db.aiReviewDao().deleteAllReviews()
        }
        seeder.seedIfEmpty()
    }
}
