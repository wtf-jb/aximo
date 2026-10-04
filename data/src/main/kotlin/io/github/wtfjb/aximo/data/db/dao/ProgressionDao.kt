package io.github.wtfjb.aximo.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.wtfjb.aximo.data.db.entity.ProgressionStateEntity

@Dao
interface ProgressionDao {
    @Query("SELECT * FROM progression_states WHERE exerciseId = :exerciseId")
    suspend fun get(exerciseId: Long): ProgressionStateEntity?

    @Upsert
    suspend fun upsert(state: ProgressionStateEntity)
}
