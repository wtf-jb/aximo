package io.github.wtfjb.aximo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI provider profile (B-01). The API key is not stored here but encrypted in
 * [io.github.wtfjb.aximo.data.ai.AiKeyStore]. Not part of the JSON backup.
 */
@Entity(tableName = "ai_profiles")
data class AiProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** [io.github.wtfjb.aximo.domain.ai.AiProviderKind] name. */
    val kind: String,
    val baseUrl: String,
    val model: String,
    val active: Boolean,
)
