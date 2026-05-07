package com.lenzbeyer.hathor.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.SafStorage
import com.lenzbeyer.hathor.data.SettingsRepository
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.domain.FilenameSanitizer
import com.lenzbeyer.hathor.domain.TrackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request

data class JobState(
    val playlistId: String? = null,
    val total: Int = 0,
    val done: Int = 0,
    val percent: Int = 0,
    val isPaused: Boolean = false,
    val activeTrackId: String? = null,
)

@Singleton
class JobManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: PlaylistRepository,
    private val settings: SettingsRepository,
    private val ytDlp: com.lenzbeyer.hathor.python.YtDlpService,
    private val ffmpeg: com.lenzbeyer.hathor.media.FFmpegTranscoder,
    private val tagger: com.lenzbeyer.hathor.media.ID3Tagger,
    private val saf: SafStorage,
    private val http: OkHttpClient,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(JobState())
    val state: StateFlow<JobState> = _state.asStateFlow()

    private var jobScope: Job? = null

    fun start(playlistId: String) {
        jobScope?.cancel()
        startService(DownloadService.ACTION_START)
        jobScope = scope.launch { runJob(playlistId) }
    }

    fun pause()  { _state.value = _state.value.copy(isPaused = true) }
    fun resume() { _state.value = _state.value.copy(isPaused = false) }
    fun cancel() {
        jobScope?.cancel()
        _state.value = JobState()
        stopService()
    }

    private suspend fun runJob(playlistId: String) {
        val maxParallel = settings.settings.first().maxParallel
        val tracks: List<TrackEntity> = repo.observeTracks(playlistId).first { it.isNotEmpty() }
        _state.value = JobState(
            playlistId = playlistId,
            total = tracks.size,
            done = tracks.count { it.status == TrackStatus.Done || it.status == TrackStatus.Skipped },
        )

        fetchCoverIfNeeded(playlistId)

        val sem = Semaphore(maxParallel)
        try {
            tracks
                .filter { !it.status.isTerminal }
                .map { track ->
                    scope.launch {
                        sem.withPermit { processTrack(track) }
                    }
                }
                .forEach { it.join() }
        } finally {
            stopService()
        }
    }

    private suspend fun fetchCoverIfNeeded(playlistId: String) {
        val playlist = repo.playlist(playlistId) ?: return
        if (!playlist.coverJpgUri.isNullOrBlank()) return
        val url = playlist.coverUrl?.takeIf { it.isNotBlank() } ?: return
        runCatching {
            val cacheFile = withContext(Dispatchers.IO) {
                val f = File(ctx.cacheDir, "covers/${playlistId}.jpg").apply {
                    parentFile?.mkdirs()
                }
                http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val body = resp.body ?: return@withContext null
                    f.outputStream().use { sink -> body.byteStream().copyTo(sink) }
                }
                f.takeIf { it.length() > 0 }
            } ?: return@runCatching
            val savedUri = saf.writeCover(playlist.playlistFolderUri, cacheFile)
            if (!savedUri.isNullOrBlank()) {
                repo.setCoverJpgUri(playlistId, savedUri)
            }
        }
    }

    private suspend fun processTrack(track: TrackEntity) {
        try {
            repo.markStatus(track.id, TrackStatus.Resolving)
            val source = ytDlp.resolveAudio(track.videoId)
                ?: error("Could not resolve audio stream")

            repo.markStatus(track.id, TrackStatus.Downloading)
            val rawCache = ytDlp.downloadStream(source)

            repo.markStatus(track.id, TrackStatus.Transcoding)
            val mp3Cache = ffmpeg.toMp3CBR320(rawCache)

            repo.markStatus(track.id, TrackStatus.Tagging)
            tagger.applyTags(mp3Cache, track)

            val playlist = repo.playlist(track.playlistId) ?: error("playlist missing")
            val fileName = FilenameSanitizer.trackFilename(track.index, track.artist, track.title)
            val savedUri = saf.writeMp3(playlist.playlistFolderUri, fileName, mp3Cache)
                ?: error("Could not write file to output folder")
            repo.setMp3Uri(track.id, savedUri)

            repo.markStatus(track.id, TrackStatus.Done)
            bumpDone()
        } catch (t: Throwable) {
            repo.markStatus(track.id, TrackStatus.Failed, t.message ?: t.javaClass.simpleName)
        }
    }

    private fun bumpDone() {
        val s = _state.value
        val newDone = s.done + 1
        _state.value = s.copy(
            done = newDone,
            percent = if (s.total == 0) 0 else (newDone * 100 / s.total),
        )
    }

    private fun startService(action: String) {
        val intent = Intent(ctx, DownloadService::class.java).setAction(action)
        ContextCompat.startForegroundService(ctx, intent)
    }

    private fun stopService() {
        ctx.stopService(Intent(ctx, DownloadService::class.java))
    }
}
