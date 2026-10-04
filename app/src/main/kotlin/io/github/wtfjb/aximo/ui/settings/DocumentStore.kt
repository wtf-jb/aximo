package io.github.wtfjb.aximo.ui.settings

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads and writes files the user picked with the Android file dialog (Storage
 * Access Framework). An interface so ViewModel tests don't need Android.
 */
interface DocumentStore {
    suspend fun write(uri: String, text: String)

    suspend fun read(uri: String): String
}

class ContentResolverDocumentStore(private val context: Context) : DocumentStore {

    override suspend fun write(uri: String, text: String) = withContext(Dispatchers.IO) {
        // "wt" truncates: overwriting an existing file must not leave old bytes at the end.
        val stream = context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: throw IOException("cannot open $uri")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }

    override suspend fun read(uri: String): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri.toUri()) ?: throw IOException("cannot open $uri")
        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
