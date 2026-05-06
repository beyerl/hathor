package com.lenzbeyer.hathor.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenzbeyer.hathor.data.Settings
import com.lenzbeyer.hathor.data.SettingsRepository
import com.lenzbeyer.hathor.python.YtDlpService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val ytDlp: YtDlpService,
) : ViewModel() {

    val settings: StateFlow<Settings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    fun setOutputFolder(uri: String) = viewModelScope.launch { repo.setOutputFolder(uri) }
    fun setMaxParallel(n: Int)       = viewModelScope.launch { repo.setMaxParallel(n) }
    fun setStripCosmetic(v: Boolean) = viewModelScope.launch { repo.setStripCosmetic(v) }

    fun loadYtDlpVersion() = viewModelScope.launch {
        runCatching { ytDlp.version() }.onSuccess { repo.setYtDlpVersion(it) }
    }

    fun updateYtDlp() = viewModelScope.launch {
        runCatching { ytDlp.updateYtDlp() }.onSuccess { repo.setYtDlpVersion(it) }
    }
}
