package com.lenzbeyer.hathor.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenzbeyer.hathor.data.PlaylistEntity
import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.SettingsRepository
import com.lenzbeyer.hathor.domain.DraftHolder
import com.lenzbeyer.hathor.python.YtDlpService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val url: String = "",
    val isWorking: Boolean = false,
    val error: String? = null,
    val draftReady: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ytDlp: YtDlpService,
    private val settings: SettingsRepository,
    private val drafts: DraftHolder,
    repo: PlaylistRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    val recent: StateFlow<List<PlaylistEntity>> = repo.observeAllPlaylists()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onUrlChange(s: String) { _ui.value = _ui.value.copy(url = s, error = null) }

    fun onContinue() = viewModelScope.launch {
        val url = _ui.value.url.trim()
        if (!isPlausibleYoutubeUrl(url)) {
            _ui.value = _ui.value.copy(error = "Doesn't look like a YouTube URL")
            return@launch
        }
        _ui.value = _ui.value.copy(isWorking = true, error = null)
        try {
            val s = settings.settings.first()
            val draft = ytDlp.enumerate(url, s.stripCosmetic)
            drafts.set(draft)
            _ui.value = _ui.value.copy(isWorking = false, draftReady = true)
        } catch (t: Throwable) {
            _ui.value = _ui.value.copy(isWorking = false, error = t.message ?: "Failed to enumerate")
        }
    }

    fun consumed() { _ui.value = _ui.value.copy(draftReady = false, url = "") }

    private fun isPlausibleYoutubeUrl(s: String): Boolean =
        s.contains("youtube.com", ignoreCase = true) || s.contains("youtu.be", ignoreCase = true)
}
