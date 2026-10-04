package io.github.wtfjb.aximo.catalog

import android.content.Context
import io.github.wtfjb.aximo.data.catalog.CatalogFile
import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.catalog.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The bundled exercise library (`assets/exercise_catalog.json`), parsed once on first use. */
class AssetCatalogRepository(private val context: Context) : CatalogRepository {
    private val mutex = Mutex()
    private var cached: List<CatalogEntry>? = null

    override suspend fun entries(): List<CatalogEntry> = mutex.withLock {
        cached ?: withContext(Dispatchers.Default) {
            context.assets.open(FILE).bufferedReader().use { CatalogFile.parse(it.readText()) }
        }.also { cached = it }
    }

    private companion object {
        const val FILE = "exercise_catalog.json"
    }
}
