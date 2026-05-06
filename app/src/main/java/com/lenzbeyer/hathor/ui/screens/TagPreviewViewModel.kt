package com.lenzbeyer.hathor.ui.screens

import androidx.lifecycle.ViewModel
import com.lenzbeyer.hathor.domain.DraftHolder
import com.lenzbeyer.hathor.domain.PlaylistDraft
import com.lenzbeyer.hathor.domain.TrackDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TagPreviewUiState(
    val draft: PlaylistDraft? = null,
)

@HiltViewModel
class TagPreviewViewModel @Inject constructor(
    drafts: DraftHolder,
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
    fun toggleSkip(idx: Int)                = updateTrack(idx) { it.copy(isSkipped = !it.isSkipped) }

    fun useChannelAsAllArtists() = update {
        copy(tracks = tracks.map { it.copy(artist = folderArtist) })
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
