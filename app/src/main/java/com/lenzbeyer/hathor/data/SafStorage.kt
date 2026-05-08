package com.lenzbeyer.hathor.data

import android.content.Context
import android.net.Uri
import android.system.ErrnoException
import android.system.OsConstants
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiskFullException(cause: Throwable? = null) : IOException("No space left on device", cause)

data class ExistingFile(val uri: String, val size: Long)

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

    /** Returns the existing file's URI + size if it's present and non-empty, else null. SPEC §10.4. */
    suspend fun findExistingMp3(playlistFolderUri: String, fileName: String): ExistingFile? =
        withContext(Dispatchers.IO) {
            val folder = DocumentFile.fromTreeUri(ctx, Uri.parse(playlistFolderUri)) ?: return@withContext null
            val doc = folder.findFile(fileName) ?: return@withContext null
            val size = doc.length()
            if (size <= 0L) null else ExistingFile(doc.uri.toString(), size)
        }

    suspend fun writeMp3(playlistFolderUri: String, fileName: String, source: File): String? = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(ctx, Uri.parse(playlistFolderUri)) ?: return@withContext null
        // Overwrite any leftover from a previous failed write. The skip-if-exists probe (SPEC §10.4)
        // happens upstream in JobManager; reaching this point means we want to write fresh content.
        folder.findFile(fileName)?.delete()
        val doc = folder.createFile("audio/mpeg", fileName) ?: return@withContext null
        try {
            ctx.contentResolver.openOutputStream(doc.uri)?.use { sink ->
                source.inputStream().use { it.copyTo(sink) }
            } ?: return@withContext null
        } catch (e: IOException) {
            throw e.toDiskFullIfApplicable()
        }
        doc.uri.toString()
    }

    suspend fun writeCover(playlistFolderUri: String, source: File): String? = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(ctx, Uri.parse(playlistFolderUri)) ?: return@withContext null
        folder.findFile("folder.jpg")?.delete()
        val doc = folder.createFile("image/jpeg", "folder.jpg") ?: return@withContext null
        try {
            ctx.contentResolver.openOutputStream(doc.uri)?.use { sink ->
                source.inputStream().use { it.copyTo(sink) }
            } ?: return@withContext null
        } catch (e: IOException) {
            throw e.toDiskFullIfApplicable()
        }
        doc.uri.toString()
    }

    /** Probes the size of a written SAF document for post-publish verification (SPEC §10.1). */
    suspend fun documentSize(uri: String): Long = withContext(Dispatchers.IO) {
        DocumentFile.fromSingleUri(ctx, Uri.parse(uri))?.length() ?: -1L
    }

    private fun IOException.toDiskFullIfApplicable(): IOException {
        var cause: Throwable? = this
        while (cause != null) {
            if (cause is ErrnoException && cause.errno == OsConstants.ENOSPC) {
                return DiskFullException(this)
            }
            cause = cause.cause
        }
        if (this.message?.contains("ENOSPC", ignoreCase = true) == true ||
            this.message?.contains("No space", ignoreCase = true) == true) {
            return DiskFullException(this)
        }
        return this
    }
}
