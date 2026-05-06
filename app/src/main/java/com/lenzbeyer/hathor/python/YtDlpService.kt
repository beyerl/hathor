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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AudioSource(
    val videoId: String,
    val streamUrl: String,
    val ext: String,
    val durationMs: Long?,
)

/** Kotlin-side wrapper around hathor_ytdlp.py and the raw audio download (OkHttp). */
@Singleton
class YtDlpService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val http: OkHttpClient,
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

    /** Resolves the bestaudio stream URL for a single video. */
    suspend fun resolveAudio(videoId: String): AudioSource? = withContext(Dispatchers.IO) {
        val raw = py.callAttr("resolve_audio_url", videoId).toString()
        val obj = JSONObject(raw)
        val url = obj.optString("url", "").ifBlank { return@withContext null }
        AudioSource(
            videoId = videoId,
            streamUrl = url,
            ext = obj.optString("ext", "m4a"),
            durationMs = obj.optLong("duration", -1).takeIf { it >= 0 }?.let { it * 1000 },
        )
    }

    /** Streams the resolved audio URL into the app cache and returns the file. */
    suspend fun downloadStream(src: AudioSource): File = withContext(Dispatchers.IO) {
        val outDir = File(ctx.cacheDir, "raw").apply { mkdirs() }
        val outFile = File(outDir, "${src.videoId}.${src.ext}")
        val req = Request.Builder().url(src.streamUrl).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} fetching audio")
            val body = resp.body ?: error("Empty response body")
            outFile.outputStream().use { sink -> body.byteStream().copyTo(sink) }
        }
        outFile
    }
}
