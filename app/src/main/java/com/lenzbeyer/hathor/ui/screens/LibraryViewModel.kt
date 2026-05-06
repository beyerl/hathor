package com.lenzbeyer.hathor.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenzbeyer.hathor.data.PlaylistEntity
import com.lenzbeyer.hathor.data.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LibraryViewModel @Inject constructor(
    repo: PlaylistRepository,
) : ViewModel() {
    val playlists: StateFlow<List<PlaylistEntity>> =
        repo.observeAllPlaylists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
