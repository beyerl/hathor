package com.lenzbeyer.hathor.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.service.JobManager
import com.lenzbeyer.hathor.service.JobState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DownloadViewModel @Inject constructor(
    state: SavedStateHandle,
    private val jobs: JobManager,
    repo: PlaylistRepository,
) : ViewModel() {

    private val playlistId: String = checkNotNull(state["playlistId"])

    val tracks: StateFlow<List<TrackEntity>> = repo.observeTracks(playlistId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<JobState> = jobs.state

    fun pause()  = jobs.pause()
    fun resume() = jobs.resume()
    fun cancel() = jobs.cancel()
}
