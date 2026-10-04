package io.github.wtfjb.aximo.ui.settings

import io.github.wtfjb.aximo.domain.backup.BackupException
import io.github.wtfjb.aximo.domain.backup.BackupRepository
import java.io.IOException

/** Backup that returns a fixed text and remembers what was restored. */
class FakeBackupRepository(private val exportText: String = "{\"schemaVersion\": 1}") : BackupRepository {
    var restored: String? = null
    var failWith: BackupException.Reason? = null

    override suspend fun exportJson(): String = exportText

    override suspend fun restoreJson(json: String) {
        failWith?.let { throw BackupException(it) }
        restored = json
    }
}

/** Files in a map, keyed by uri. A missing file fails like an unreadable one. */
class FakeDocumentStore : DocumentStore {
    val files = mutableMapOf<String, String>()

    override suspend fun write(uri: String, text: String) {
        files[uri] = text
    }

    override suspend fun read(uri: String): String = files[uri] ?: throw IOException("no file $uri")
}
