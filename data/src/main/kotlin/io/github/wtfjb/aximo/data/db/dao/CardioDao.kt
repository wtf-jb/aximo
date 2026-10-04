package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.wtfjb.aximo.data.db.entity.CardioEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardioDao {
    @Query("SELECT * FROM cardio_entries ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<CardioEntryEntity>>

    @Insert
    suspend fun insert(entry: CardioEntryEntity): Long

    @Update
    suspend fun update(entry: CardioEntryEntity)

    @Query("DELETE FROM cardio_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
