package io.github.wtfjb.aximo.data.repository

import io.github.wtfjb.aximo.data.db.dao.CardioDao
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.repository.CardioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCardioRepository(private val dao: CardioDao) : CardioRepository {

    override fun observeRecent(limit: Int): Flow<List<CardioEntry>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeAll(): Flow<List<CardioEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getEntry(id: Long): CardioEntry? = dao.getById(id)?.toDomain()

    override suspend fun saveEntry(entry: CardioEntry): Long =
        if (entry.id == 0L) {
            dao.insert(entry.toEntity())
        } else {
            dao.update(entry.toEntity())
            entry.id
        }

    override suspend fun deleteEntry(id: Long) = dao.delete(id)
}
