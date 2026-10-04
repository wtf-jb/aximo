package io.github.wtfjb.aximo.domain.repository

import io.github.wtfjb.aximo.domain.model.CardioEntry
import kotlinx.coroutines.flow.Flow

/** Access to cardio entries (A-04). Implemented in :data. */
interface CardioRepository {
    /** The newest [limit] entries, newest first. */
    fun observeRecent(limit: Int): Flow<List<CardioEntry>>

    suspend fun getEntry(id: Long): CardioEntry?

    /** Inserts (id = 0) or updates the entry. Returns the id. */
    suspend fun saveEntry(entry: CardioEntry): Long

    suspend fun deleteEntry(id: Long)
}
