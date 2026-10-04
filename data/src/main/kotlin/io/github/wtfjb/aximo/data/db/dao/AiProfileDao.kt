package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.wtfjb.aximo.data.db.entity.AiProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProfileDao {
    @Query("SELECT * FROM ai_profiles ORDER BY id")
    fun observeAll(): Flow<List<AiProfileEntity>>

    @Query("SELECT * FROM ai_profiles WHERE id = :id")
    suspend fun getById(id: Long): AiProfileEntity?

    @Query("SELECT COUNT(*) FROM ai_profiles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(profile: AiProfileEntity): Long

    @Update
    suspend fun update(profile: AiProfileEntity)

    @Query("DELETE FROM ai_profiles WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE ai_profiles SET active = (id = :id)")
    suspend fun setActive(id: Long)

    /** Inserts; the first profile becomes active. */
    @Transaction
    suspend fun insertNew(profile: AiProfileEntity): Long =
        insert(profile.copy(active = profile.active || count() == 0))
}
