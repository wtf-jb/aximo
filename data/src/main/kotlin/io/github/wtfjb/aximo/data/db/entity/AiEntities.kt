package io.github.wtfjb.aximo.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlin.time.Instant

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

/** A weekly review (B-02). */
@Entity(tableName = "ai_reviews")
data class AiReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Instant,
    val weeks: Int,
    val summary: String,
    val droppedSuggestions: Int,
)

/**
 * A suggestion of a review. [type] names the kind of change, [payloadJson]
 * holds its values ([io.github.wtfjb.aximo.data.ai.SuggestionPayload]).
 */
@Entity(
    tableName = "ai_suggestions",
    foreignKeys = [
        ForeignKey(entity = AiReviewEntity::class, parentColumns = ["id"], childColumns = ["reviewId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("reviewId")],
)
data class AiSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reviewId: Long,
    val position: Int,
    val type: String,
    val payloadJson: String,
    val rationale: String,
    /** [io.github.wtfjb.aximo.domain.review.SuggestionStatus] name. */
    val status: String,
)

data class AiReviewWithSuggestions(
    @Embedded val review: AiReviewEntity,
    @Relation(parentColumn = "id", entityColumn = "reviewId")
    val suggestions: List<AiSuggestionEntity>,
)
