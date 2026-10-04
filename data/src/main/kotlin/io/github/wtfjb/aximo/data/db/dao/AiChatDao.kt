package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.wtfjb.aximo.data.db.entity.AiChatMessageEntity
import io.github.wtfjb.aximo.data.db.entity.AiChatMessageWithSuggestions
import io.github.wtfjb.aximo.data.db.entity.AiSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {
    @Transaction
    @Query("SELECT * FROM ai_chat_messages ORDER BY createdAt, id")
    fun observeMessages(): Flow<List<AiChatMessageWithSuggestions>>

    @Insert
    suspend fun insertMessage(message: AiChatMessageEntity): Long

    @Insert
    suspend fun insertSuggestions(suggestions: List<AiSuggestionEntity>)

    @Query("DELETE FROM ai_suggestions WHERE chatMessageId IS NOT NULL")
    suspend fun deleteChatSuggestions()

    @Query("DELETE FROM ai_chat_messages")
    suspend fun deleteMessages()

    /** Question and answer with the answer's suggestions, all or nothing. */
    @Transaction
    suspend fun saveExchange(question: AiChatMessageEntity, answer: AiChatMessageEntity, suggestions: List<AiSuggestionEntity>) {
        insertMessage(question)
        val answerId = insertMessage(answer)
        insertSuggestions(suggestions.map { it.copy(chatMessageId = answerId) })
    }

    @Transaction
    suspend fun clear() {
        deleteChatSuggestions()
        deleteMessages()
    }
}
