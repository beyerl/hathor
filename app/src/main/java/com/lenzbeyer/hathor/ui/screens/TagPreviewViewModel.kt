package com.lenzbeyer.hathor.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenzbeyer.hathor.data.PlaylistEntity
import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.SafStorage
import com.lenzbeyer.hathor.data.SettingsRepository
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.domain.DraftHolder
import com.lenzbeyer.hathor.domain.FilenameSanitizer
import com.lenzbeyer.hathor.domain.PlaylistDraft
import com.lenzbeyer.hathor.domain.TrackDraft
import com.lenzbeyer.hathor.domain.TrackStatus
import com.lenzbeyer.hathor.service.JobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TagPreviewUiState(
    val draft: PlaylistDraft? = null,
    val error: String? = null,
)

@HiltViewModel
class TagPreviewViewModel @Inject constructor(
    drafts: DraftHolder,
    private val repo: PlaylistRepository,
    private val settings: SettingsRepository,
    private val jobs: JobManager,
    private val saf: SafStorage,
) : ViewModel() {

    private val _ui = MutableStateFlow(TagPreviewUiState(draft = drafts.draft.value))
    val ui: StateFlow<TagPreviewUiState> = _ui.asStateFlow()

    fun setFolderArtist(v: String) = update { copy(folderArtist = v) }
    fun setTitle(v: String)        = update { copy(title = v) }
    fun setAlbum(v: String)        = update { copy(album = v) }
    fun setYear(v: String?)        = update { copy(year = v?.takeIf { it.isNotBlank() }) }
    fun setGenre(v: String?)       = update { copy(genre = v?.takeIf { it.isNotBlank() }) }

    fun setTrackArtist(idx: Int, v: String) = updateTrack(idx) { it.copy(artist = v) }
    fun setTrackTitle(idx: Int, v: String)  = updateTrack(idx) { it.copy(title = v) }
    fun toggleInclude(idx: Int)             = updateTrack(idx) { it.copy(isSkipped = !it.isSkipped) }

    fun useChannelAsAllArtists() = update {
        copy(tracks = tracks.map { it.copy(artist = folderArtist) })
    }

    fun startDownload(onStarted: (playlistId: String) -> Unit) {
        val draft = _ui.value.draft ?: return
        viewModelScope.launch {
            val rootFolderUri = settings.settings.first().outputFolderUri
            if (rootFolderUri.isNullOrBlank()) {
                _ui.value = _ui.value.copy(error = "Pick an output folder in Settings first.")
                return@launch
            }

            val folderName = FilenameSanitizer.playlistFolderName(draft.folderArtist, draft.title)
            val playlistFolderUri = saf.ensurePlaylistFolder(rootFolderUri, folderName)
            if (playlistFolderUri.isNullOrBlank()) {
                _ui.value = _ui.value.copy(error = "Could not create folder in the chosen output location.")
                return@launch
            }

            val now = System.currentTimeMillis()
            val playlist = PlaylistEntity(
                id = draft.youtubePlaylistId,
                url = draft.sourceUrl,
                title = draft.title,
                folderArtist = draft.folderArtist,
                album = draft.album,
                year = draft.year,
                genre = draft.genre,
                rootFolderUri = rootFolderUri,
                playlistFolderUri = playlistFolderUri,
                coverUrl = draft.coverUrl,
                coverJpgUri = null,
                createdAt = now,
                lastSyncedAt = null,
            )
            val total = draft.tracks.size
            val tracks = draft.tracks.map { t ->
                TrackEntity(
                    id = "${draft.youtubePlaylistId}:${t.videoId}",
                    playlistId = draft.youtubePlaylistId,
                    videoId = t.videoId,
                    index = t.originalIndex,
                    title = t.title,
                    artist = t.artist,
                    trackTotal = total,
                    mp3Uri = null,
                    status = if (t.isSkipped) TrackStatus.Skipped else TrackStatus.Pending,
                    errorMessage = null,
                    durationMs = t.durationMs,
                    sourceFormat = null,
                    updatedAt = now,
                )
            }
            repo.upsertPlaylist(playlist)
            repo.upsertTracks(tracks)
            jobs.start(draft.youtubePlaylistId)
            onStarted(draft.youtubePlaylistId)
        }
    }

    private inline fun update(crossinline f: PlaylistDraft.() -> PlaylistDraft) {
        val cur = _ui.value.draft ?: return
        _ui.value = _ui.value.copy(draft = cur.f())
    }

    private inline fun updateTrack(idx: Int, crossinline f: (TrackDraft) -> TrackDraft) {
        val cur = _ui.value.draft ?: return
        _ui.value = _ui.value.copy(
            draft = cur.copy(
                tracks = cur.tracks.mapIndexed { i, t -> if (i == idx) f(t) else t }
            )
        )
    }
}
