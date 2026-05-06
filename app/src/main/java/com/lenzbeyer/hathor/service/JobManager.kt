package com.lenzbeyer.hathor.service

import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.SettingsRepository
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.domain.TrackStatus
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class JobState(
    val playlistId: String? = null,
    val total: Int = 0,
    val done: Int = 0,
    val percent: Int = 0,
    val isPaused: Boolean = false,
    val activeTrackId: String? = null,
)

/**
 * Orchestrates the per-track pipeline (SPEC §10). Concurrency via Semaphore. Pause/cancel
 * cooperate via the SupervisorJob — running tasks finish their current step then exit.
 *
 * NOTE: actual yt-dlp/FFmpegKit/Tagger calls are skeletons in v0.1; they update Track status
 * through the repository so the UI flows render correctly. Wiring real audio fetch + transcode
 * + tag is the next milestone after this skeleton lands.
 */
@Singleton
class JobManager @Inject constructor(
    private val repo: PlaylistRepository,
    private val settings: SettingsRepository,
    private val ytDlp: com.lenzbeyer.hathor.python.YtDlpService,
    private val ffmpeg: com.lenzbeyer.hathor.media.FFmpegTranscoder,
    private val tagger: com.lenzbeyer.hathor.media.ID3Tagger,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(JobState())
    val state: StateFlow<JobState> = _state.asStateFlow()

    private var jobScope: Job? = null

    fun start(playlistId: String) {
        jobScope?.cancel()
        jobScope = scope.launch { runJob(playlistId) }
    }

    fun pause()  { _state.value = _state.value.copy(isPaused = true) }
    fun resume() { _state.value = _state.value.copy(isPaused = false) }
    fun cancel() { jobScope?.cancel(); _state.value = JobState() }

    private suspend fun runJob(playlistId: String) {
        val maxParallel = settings.settings.first().maxParallel
        // Snapshot once; not actively listening for upstream changes during a job.
        val tracks: List<TrackEntity> = repo.observeTracks(playlistId).first { it.isNotEmpty() }
        _state.value = JobState(
            playlistId = playlistId,
            total = tracks.size,
            done = tracks.count { it.status == TrackStatus.Done || it.status == TrackStatus.Skipped },
        )
        val sem = Semaphore(maxParallel)
        tracks
            .filter { !it.status.isTerminal }
            .map { track ->
                scope.launch {
                    sem.withPermit { processTrack(track) }
                }
            }
            .forEach { it.join() }
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

            // TODO: copy mp3Cache → SAF playlistFolderUri / track filename, then setMp3Uri.
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
}
