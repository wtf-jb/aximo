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

/** A message of the coach chat (B-05). Not part of the JSON backup. */
@Entity(tableName = "ai_chat_messages")
data class AiChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [io.github.wtfjb.aximo.domain.chat.ChatRole] name. */
    val role: String,
    val text: String,
    val createdAt: Instant,
    val droppedSuggestions: Int,
)

/**
 * A suggestion of a review or of a chat answer: exactly one of [reviewId] and
 * [chatMessageId] is set. [type] names the kind of change, [payloadJson] holds
 * its values ([io.github.wtfjb.aximo.data.ai.SuggestionPayload]).
 */
@Entity(
    tableName = "ai_suggestions",
    foreignKeys = [
        ForeignKey(entity = AiReviewEntity::class, parentColumns = ["id"], childColumns = ["reviewId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AiChatMessageEntity::class, parentColumns = ["id"], childColumns = ["chatMessageId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("reviewId"), Index("chatMessageId")],
)
data class AiSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reviewId: Long?,
    val position: Int,
    val type: String,
    val payloadJson: String,
    val rationale: String,
    /** [io.github.wtfjb.aximo.domain.review.SuggestionStatus] name. */
    val status: String,
    val chatMessageId: Long? = null,
    /** [io.github.wtfjb.aximo.domain.review.SuggestionReason] name, null if the AI gave none. */
    val reason: String? = null,
)

data class AiReviewWithSuggestions(
    @Embedded val review: AiReviewEntity,
    @Relation(parentColumn = "id", entityColumn = "reviewId")
    val suggestions: List<AiSuggestionEntity>,
)

data class AiChatMessageWithSuggestions(
    @Embedded val message: AiChatMessageEntity,
    @Relation(parentColumn = "id", entityColumn = "chatMessageId")
    val suggestions: List<AiSuggestionEntity>,
)
