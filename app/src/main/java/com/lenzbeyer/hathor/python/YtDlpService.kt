package com.lenzbeyer.hathor.python

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.lenzbeyer.hathor.domain.PlaylistDraft
import com.lenzbeyer.hathor.domain.TagParser
import com.lenzbeyer.hathor.domain.TrackDraft
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Kotlin-side wrapper around hathor_ytdlp.py. */
@Singleton
class YtDlpService @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val py: PyObject by lazy { Python.getInstance().getModule("hathor_ytdlp") }

    suspend fun version(): String = withContext(Dispatchers.IO) {
        py.callAttr("ytdlp_version").toString()
    }

    suspend fun updateYtDlp(): String = withContext(Dispatchers.IO) {
        py.callAttr("update_ytdlp").toString()
    }

    /** Enumerates a playlist URL, producing a draft pre-populated by the smart-tag parser. */
    suspend fun enumerate(url: String, stripCosmetic: Boolean): PlaylistDraft = withContext(Dispatchers.IO) {
        val raw = py.callAttr("enumerate_playlist", url).toString()
        val obj = JSONObject(raw)
        if (obj.has("error")) error(obj.getString("error"))

        val entries = obj.getJSONArray("entries")
        val drafts = mutableListOf<TrackDraft>()
        val parsedArtists = mutableListOf<String>()
        var year: String? = null
        var genre: String? = null

        for (i in 0 until entries.length()) {
            val e = entries.getJSONObject(i)
            val parsed = TagParser.parse(
                rawTitle      = e.optStringOrNull("title")   ?: "",
                uploader      = e.optStringOrNull("uploader") ?: "",
                ytdlpArtist   = e.optStringOrNull("ytdlp_artist"),
                ytdlpTrack    = e.optStringOrNull("ytdlp_track"),
                stripCosmetic = stripCosmetic,
            )
            parsedArtists += parsed.artist

            if (year == null) {
                year = e.optStringOrNull("release_year")
                    ?: e.optStringOrNull("upload_date")?.take(4)?.takeIf { it.isNotBlank() }
            }
            if (genre == null) genre = e.optStringOrNull("genre")

            drafts += TrackDraft(
                videoId       = e.getString("id"),
                originalIndex = i + 1,
                artist        = parsed.artist,
                title         = parsed.title,
                durationMs    = e.optLong("duration", -1).takeIf { it >= 0 }?.let { it * 1000 },
            )
        }

        val firstUploader = entries.optJSONObject(0)?.optStringOrNull("uploader").orEmpty()
        val folderArtist = com.lenzbeyer.hathor.domain.FolderArtistResolver.resolve(
            entries = parsedArtists.map {
                com.lenzbeyer.hathor.domain.FolderArtistResolver.Entry(it, firstUploader)
            }
        )

        PlaylistDraft(
            sourceUrl         = url,
            youtubePlaylistId = obj.optStringOrNull("id") ?: "playlist-${url.hashCode()}",
            title             = obj.optStringOrNull("title") ?: "Untitled",
            folderArtist      = folderArtist,
            album             = obj.optStringOrNull("title") ?: "Untitled",
            year              = year,
            genre             = genre,
            coverUrl          = obj.optStringOrNull("thumbnail")
                ?: entries.optJSONObject(0)?.optStringOrNull("thumbnail"),
            tracks            = drafts,
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotEmpty() } else null

    /** Downloads the bestaudio stream for a single video into the app cache. */
    suspend fun downloadAudio(videoId: String): File = withContext(Dispatchers.IO) {
        val outDir = File(ctx.cacheDir, "raw").apply { mkdirs() }
        val raw = py.callAttr("download_audio", videoId, outDir.absolutePath).toString()
        val obj = JSONObject(raw)
        if (obj.has("error")) error(obj.getString("error"))
        val path = obj.optString("path", "").ifBlank { error("yt-dlp returned no file path") }
        val file = File(path)
        if (!file.exists() || file.length() == 0L) error("Downloaded file missing or empty: $path")
        file
    }
}
