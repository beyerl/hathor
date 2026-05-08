package com.lenzbeyer.hathor.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenzbeyer.hathor.data.PlaylistEntity
import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.SettingsRepository
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.domain.TrackStatus
import com.lenzbeyer.hathor.python.YtDlpService
import com.lenzbeyer.hathor.service.JobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    state: SavedStateHandle,
    private val repo: PlaylistRepository,
    private val settings: SettingsRepository,
    private val ytDlp: YtDlpService,
    private val jobs: JobManager,
) : ViewModel() {

    private val playlistId: String = checkNotNull(state["playlistId"])

    val playlist: StateFlow<PlaylistEntity?> = repo.observePlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tracks: StateFlow<List<TrackEntity>> = repo.observeTracks(playlistId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _resyncError = MutableStateFlow<String?>(null)
    val resyncError: StateFlow<String?> = _resyncError.asStateFlow()

    /**
     * SPEC §10.4 — re-enumerate via yt-dlp, merge new tracks by videoId (keep existing mp3Uri),
     * and start the job. JobManager's skip-if-exists check skips already-on-disk tracks.
     */
    fun resync() {
        val p = playlist.value ?: return
        viewModelScope.launch {
            _resyncError.value = null
            val s = settings.settings.first()
            val draft = try {
                ytDlp.enumerate(p.url, s.stripCosmetic)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                _resyncError.value = e.message ?: "Re-sync failed"
                return@launch
            }

            val existing: Map<String, TrackEntity> = tracks.value.associateBy { it.videoId }
            val now = System.currentTimeMillis()

            // Renumber post-skip per SPEC §8.3 / §9.3.
            val kept = draft.tracks.filterNot { it.isSkipped }
            val keptIndex = kept.mapIndexed { i, t -> t.videoId to (i + 1) }.toMap()
            val keptTotal = kept.size

            val merged = draft.tracks.map { t ->
                val prior = existing[t.videoId]
                TrackEntity(
                    id = "${draft.youtubePlaylistId}:${t.videoId}",
                    playlistId = draft.youtubePlaylistId,
                    videoId = t.videoId,
                    index = keptIndex[t.videoId] ?: 0,
                    title = prior?.title ?: t.title,
                    artist = prior?.artist ?: t.artist,
                    trackTotal = keptTotal,
                    mp3Uri = prior?.mp3Uri,
                    status = if (t.isSkipped) TrackStatus.Skipped else TrackStatus.Pending,
                    errorMessage = null,
                    durationMs = t.durationMs,
                    sourceFormat = prior?.sourceFormat,
                    updatedAt = now,
                )
            }

            repo.upsertTracks(merged)
            jobs.start(playlistId)
        }
    }
}
