package com.lenzbeyer.hathor.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class SafStorage @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    suspend fun ensurePlaylistFolder(rootFolderUri: String, folderName: String): String? = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(ctx, Uri.parse(rootFolderUri)) ?: return@withContext null
        val existing = root.findFile(folderName)
        val folder = existing?.takeIf { it.isDirectory } ?: root.createDirectory(folderName)
        folder?.uri?.toString()
    }

    suspend fun writeMp3(playlistFolderUri: String, fileName: String, source: File): String? = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(ctx, Uri.parse(playlistFolderUri)) ?: return@withContext null
        folder.findFile(fileName)?.delete()
        val doc = folder.createFile("audio/mpeg", fileName) ?: return@withContext null
        ctx.contentResolver.openOutputStream(doc.uri)?.use { sink ->
            source.inputStream().use { it.copyTo(sink) }
        } ?: return@withContext null
        doc.uri.toString()
    }

    suspend fun writeCover(playlistFolderUri: String, source: File): String? = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(ctx, Uri.parse(playlistFolderUri)) ?: return@withContext null
        folder.findFile("folder.jpg")?.delete()
        val doc = folder.createFile("image/jpeg", "folder.jpg") ?: return@withContext null
        ctx.contentResolver.openOutputStream(doc.uri)?.use { sink ->
            source.inputStream().use { it.copyTo(sink) }
        } ?: return@withContext null
        doc.uri.toString()
    }
}
