package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.wtfjb.aximo.data.db.entity.AiReviewEntity
import io.github.wtfjb.aximo.data.db.entity.AiReviewWithSuggestions
import io.github.wtfjb.aximo.data.db.entity.AiSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiReviewDao {
    @Transaction
    @Query("SELECT * FROM ai_reviews ORDER BY createdAt DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<AiReviewWithSuggestions?>

    @Insert
    suspend fun insertReview(review: AiReviewEntity): Long

    @Insert
    suspend fun insertSuggestions(suggestions: List<AiSuggestionEntity>)

    @Query("SELECT * FROM ai_suggestions WHERE id = :id")
    suspend fun getSuggestion(id: Long): AiSuggestionEntity?

    @Query("UPDATE ai_suggestions SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    /** Reviews and chat messages are deleted afterwards; suggestions point to them. */
    @Query("DELETE FROM ai_suggestions")
    suspend fun deleteAllSuggestions()

    @Query("DELETE FROM ai_reviews")
    suspend fun deleteAllReviews()

    /** Saves the review with its suggestions, all or nothing. */
    @Transaction
    suspend fun save(review: AiReviewEntity, suggestions: List<AiSuggestionEntity>): Long {
        val id = insertReview(review)
        insertSuggestions(suggestions.map { it.copy(reviewId = id) })
        return id
    }
}
