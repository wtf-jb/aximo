package io.github.wtfjb.aximo.catalog

import android.content.Context
import io.github.wtfjb.aximo.data.catalog.CatalogFile
import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.catalog.CatalogRepository
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The bundled exercise library (`assets/exercise_catalog.json`), parsed once on first use.
 * On a German device names and steps come from `exercise_catalog_de.json`.
 */
class AssetCatalogRepository(private val context: Context) : CatalogRepository {
    private val mutex = Mutex()
    private var cached: List<CatalogEntry>? = null

    override suspend fun entries(): List<CatalogEntry> = mutex.withLock {
        cached ?: withContext(Dispatchers.Default) {
            val entries = CatalogFile.parse(readAsset(FILE))
            if (Locale.getDefault().language == GERMAN) CatalogFile.translate(entries, readAsset(FILE_DE)) else entries
        }.also { cached = it }
    }

    private fun readAsset(name: String): String = context.assets.open(name).bufferedReader().use { it.readText() }

    private companion object {
        const val FILE = "exercise_catalog.json"
        const val FILE_DE = "exercise_catalog_de.json"
        const val GERMAN = "de"
    }
}
