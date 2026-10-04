package io.github.wtfjb.aximo.ui.cardio

import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.repository.CardioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory cardio repository for ViewModel tests. */
class FakeCardioRepository(initial: List<CardioEntry> = emptyList()) : CardioRepository {
    private val entries = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val all: List<CardioEntry> get() = entries.value

    override fun observeRecent(limit: Int): Flow<List<CardioEntry>> =
        entries.map { list -> list.sortedByDescending { it.startedAt }.take(limit) }

    override fun observeAll(): Flow<List<CardioEntry>> = entries.map { list -> list.sortedByDescending { it.startedAt } }

    override suspend fun getEntry(id: Long): CardioEntry? = entries.value.find { it.id == id }

    override suspend fun saveEntry(entry: CardioEntry): Long {
        val id = if (entry.id == 0L) nextId++ else entry.id
        entries.value = entries.value.filter { it.id != id } + entry.copy(id = id)
        return id
    }

    override suspend fun deleteEntry(id: Long) {
        entries.value = entries.value.filter { it.id != id }
    }
}
