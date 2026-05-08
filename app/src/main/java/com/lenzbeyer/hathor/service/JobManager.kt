package com.lenzbeyer.hathor.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import com.lenzbeyer.hathor.data.DiskFullException
import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.SafStorage
import com.lenzbeyer.hathor.data.SettingsRepository
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.domain.FilenameSanitizer
import com.lenzbeyer.hathor.domain.TrackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class PauseReason { Manual, Network, DiskFull }

data class JobState(
    val playlistId: String? = null,
    val total: Int = 0,
    val done: Int = 0,
    val percent: Int = 0,
    val isPaused: Boolean = false,
    val pauseReason: PauseReason? = null,
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
    private var sem: Semaphore? = null
    private var currentPlaylistId: String? = null

    private val connectivity by lazy {
        ctx.getSystemService(ConnectivityManager::class.java)
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            // Auto-pause only if user hasn't manually paused for another reason.
            val s = _state.value
            if (!s.isPaused) pauseInternal(PauseReason.Network)
        }
        override fun onAvailable(network: Network) {
            val s = _state.value
            if (s.isPaused && s.pauseReason == PauseReason.Network) resume()
        }
    }
    private var networkCallbackRegistered = false

    fun start(playlistId: String) {
        jobScope?.cancel()
        currentPlaylistId = playlistId
        registerNetworkCallback()
        startService(DownloadService.ACTION_START)
        jobScope = scope.launch { runJob(playlistId) }
    }

    fun pause()  = pauseInternal(PauseReason.Manual)

    fun resume() {
        _state.value = _state.value.copy(isPaused = false, pauseReason = null)
    }

    fun cancel() {
        jobScope?.cancel()
        jobScope = null
        unregisterNetworkCallback()
        currentPlaylistId = null
        _state.value = JobState()
        stopService()
    }

    /** Re-runs a single failed track. Re-uses the active job's semaphore if a job is running;
     *  otherwise spins up a one-track mini-job. SPEC §6.3 retry. */
    fun retry(trackId: String) {
        scope.launch {
            val track = repo.observeTracks(currentPlaylistIdOrInfer(trackId) ?: return@launch)
                .first()
                .firstOrNull { it.id == trackId } ?: return@launch
            repo.markStatus(trackId, TrackStatus.Pending)
            val activeSem = sem
            if (activeSem != null) {
                scope.launch { activeSem.withPermit { processTrack(track) } }
            } else {
                // No active job — start a singleton.
                currentPlaylistId = track.playlistId
                registerNetworkCallback()
                startService(DownloadService.ACTION_START)
                jobScope = scope.launch {
                    val s = Semaphore(1)
                    sem = s
                    try {
                        s.withPermit { processTrack(track) }
                    } finally {
                        sem = null
                        stopService()
                    }
                }
            }
        }
    }

    private fun currentPlaylistIdOrInfer(trackId: String): String? =
        currentPlaylistId ?: trackId.substringBefore(":").takeIf { it.isNotBlank() }

    private suspend fun runJob(playlistId: String) {
        val maxParallel = settings.settings.first().maxParallel
        val tracks: List<TrackEntity> = repo.observeTracks(playlistId).first { it.isNotEmpty() }
        _state.value = JobState(
            playlistId = playlistId,
            total = tracks.size,
            done = tracks.count { it.status == TrackStatus.Done || it.status == TrackStatus.Skipped },
        )

        fetchCoverIfNeeded(playlistId)

        val s = Semaphore(maxParallel)
        sem = s
        try {
            tracks
                .filter { !it.status.isTerminal }
                .map { track ->
                    scope.launch {
                        s.withPermit { processTrack(track) }
                    }
                }
                .forEach { it.join() }
            repo.touchSync(playlistId)
        } finally {
            sem = null
            stopService()
        }
    }

    private suspend fun fetchCoverIfNeeded(playlistId: String) {
        val playlist = repo.playlist(playlistId) ?: return
        if (!playlist.coverJpgUri.isNullOrBlank()) return
        val url = playlist.coverUrl?.takeIf { it.isNotBlank() } ?: return
        val cacheFile = httpDownloadWithRetry(url, File(ctx.cacheDir, "covers/${playlistId}.jpg"))
            ?: return
        val savedUri = saf.writeCover(playlist.playlistFolderUri, cacheFile)
        if (!savedUri.isNullOrBlank()) {
            repo.setCoverJpgUri(playlistId, savedUri)
        }
        cacheFile.delete()
    }

    /** SPEC §10.5 — 3 attempts with exponential backoff (1s, 4s, 16s). */
    private suspend fun httpDownloadWithRetry(url: String, dest: File): File? {
        val backoffsMs = longArrayOf(1_000L, 4_000L, 16_000L)
        for (attempt in 0..2) {
            try {
                return withContext(Dispatchers.IO) {
                    dest.parentFile?.mkdirs()
                    http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            // 4xx — don't retry; non-recoverable
                            if (resp.code in 400..499) return@withContext null
                            throw IOException("HTTP ${resp.code}")
                        }
                        val body = resp.body ?: return@withContext null
                        dest.outputStream().use { sink -> body.byteStream().copyTo(sink) }
                        dest.takeIf { it.length() > 0 }
                    }
                }
            } catch (_: IOException) {
                if (attempt == 2) return null
                delay(backoffsMs[attempt])
            }
        }
        return null
    }

    private suspend fun processTrack(track: TrackEntity) {
        val playlist = repo.playlist(track.playlistId) ?: return
        val rawCache: File?
        val mp3Cache: File?
        try {
            awaitNotPaused()
            repo.markStatus(track.id, TrackStatus.Resolving)

            val fileName = FilenameSanitizer.trackFilename(track.index, track.artist, track.title)

            // SPEC §10.4 skip-if-exists.
            saf.findExistingMp3(playlist.playlistFolderUri, fileName)?.let { existing ->
                repo.setMp3Uri(track.id, existing.uri)
                repo.markStatus(track.id, TrackStatus.Skipped)
                bumpDone()
                return
            }

            awaitNotPaused()
            repo.markStatus(track.id, TrackStatus.Downloading)
            rawCache = ytDlp.downloadAudio(track.videoId)

            awaitNotPaused()
            repo.markStatus(track.id, TrackStatus.Transcoding)
            mp3Cache = ffmpeg.toMp3CBR320(rawCache)

            awaitNotPaused()
            repo.markStatus(track.id, TrackStatus.Tagging)
            tagger.applyTags(mp3Cache, track)

            awaitNotPaused()
            val savedUri = saf.writeMp3(playlist.playlistFolderUri, fileName, mp3Cache)
                ?: error("Could not write file to output folder")

            // SPEC §10.1 publishing — verify size matches the source we just wrote.
            val written = saf.documentSize(savedUri)
            if (written != mp3Cache.length()) {
                error("SAF write size mismatch: expected ${mp3Cache.length()}, got $written")
            }

            repo.setMp3Uri(track.id, savedUri)
            repo.markStatus(track.id, TrackStatus.Done)
            bumpDone()

            // SPEC §10.1 — delete cache after successful publish.
            rawCache.delete()
            mp3Cache.delete()
        } catch (d: DiskFullException) {
            // SPEC §10.5 disk-full → pause job.
            repo.markStatus(track.id, TrackStatus.Failed, "Disk full")
            pauseInternal(PauseReason.DiskFull)
        } catch (t: Throwable) {
            repo.markStatus(track.id, TrackStatus.Failed, t.message ?: t.javaClass.simpleName)
        }
    }

    /** Suspends while the job is paused. Wakes on any resume. SPEC §10.2 / §10.3 / §10.5. */
    private suspend fun awaitNotPaused() {
        if (!_state.value.isPaused) return
        _state.first { !it.isPaused }
    }

    private fun pauseInternal(reason: PauseReason) {
        _state.value = _state.value.copy(isPaused = true, pauseReason = reason)
    }

    private fun bumpDone() {
        val s = _state.value
        val newDone = s.done + 1
        _state.value = s.copy(
            done = newDone,
            percent = if (s.total == 0) 0 else (newDone * 100 / s.total),
        )
    }

    private fun registerNetworkCallback() {
        if (networkCallbackRegistered) return
        val cm = connectivity ?: return
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { cm.registerNetworkCallback(req, networkCallback) }
            .onSuccess { networkCallbackRegistered = true }
    }

    private fun unregisterNetworkCallback() {
        if (!networkCallbackRegistered) return
        runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
        networkCallbackRegistered = false
    }

    private fun startService(action: String) {
        val intent = Intent(ctx, DownloadService::class.java).setAction(action)
        ContextCompat.startForegroundService(ctx, intent)
    }

    private fun stopService() {
        ctx.stopService(Intent(ctx, DownloadService::class.java))
        unregisterNetworkCallback()
    }
}
