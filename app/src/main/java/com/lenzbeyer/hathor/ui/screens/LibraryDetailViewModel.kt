package com.lenzbeyer.hathor.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenzbeyer.hathor.data.PlaylistEntity
import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.TrackEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    state: SavedStateHandle,
    repo: PlaylistRepository,
) : ViewModel() {

    private val playlistId: String = checkNotNull(state["playlistId"])

    val playlist: StateFlow<PlaylistEntity?> = repo.observePlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tracks: StateFlow<List<TrackEntity>> = repo.observeTracks(playlistId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
