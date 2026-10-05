package io.github.wtfjb.aximo.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import io.github.wtfjb.aximo.data.db.AximoDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Auto-migrations keep existing rows (checked against the exported schemas). */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AximoDatabase::class.java)

    @Test
    fun from3To4KeepsReviewSuggestions() {
        helper.createDatabase(DB, 3).apply {
            execSQL("INSERT INTO ai_reviews (id, createdAt, weeks, summary, droppedSuggestions) VALUES (1, 1000, 6, 'Gut', 0)")
            execSQL(
                "INSERT INTO ai_suggestions (id, reviewId, position, type, payloadJson, rationale, status) " +
                    "VALUES (5, 1, 0, 'SET_COUNT', '{\"routineId\":1,\"exerciseId\":2,\"from\":3,\"to\":4}', 'weil', 'OPEN')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 4, true)

        db.query("SELECT id, reviewId, chatMessageId, rationale FROM ai_suggestions").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(5L, c.getLong(0))
            assertEquals(1L, c.getLong(1))
            assertTrue(c.isNull(2))
            assertEquals("weil", c.getString(3))
        }
        db.query("SELECT COUNT(*) FROM ai_chat_messages").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun from4To5AddsAnEmptyReason() {
        helper.createDatabase(DB, 4).apply {
            execSQL("INSERT INTO ai_reviews (id, createdAt, weeks, summary, droppedSuggestions) VALUES (1, 1000, 6, 'Gut', 0)")
            execSQL(
                "INSERT INTO ai_suggestions (id, reviewId, position, type, payloadJson, rationale, status) " +
                    "VALUES (5, 1, 0, 'SET_COUNT', '{\"routineId\":1,\"exerciseId\":2,\"from\":3,\"to\":4}', 'weil', 'OPEN')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 5, true)

        db.query("SELECT id, reviewId, rationale, reason FROM ai_suggestions").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(5L, c.getLong(0))
            assertEquals(1L, c.getLong(1))
            assertEquals("weil", c.getString(2))
            assertTrue(c.isNull(3))
        }
        db.execSQL("UPDATE ai_suggestions SET reason = 'volume_low' WHERE id = 5")
        db.close()
    }

    private companion object {
        const val DB = "migration-test"
    }
}
