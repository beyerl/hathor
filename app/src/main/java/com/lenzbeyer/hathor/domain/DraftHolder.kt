package com.lenzbeyer.hathor.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single shared cell holding the in-flight PlaylistDraft between the Home and TagPreview
 * screens. v0.1 — replace with a navGraph-scoped ViewModel or a "pending" Room row in v0.2.
 */
@Singleton
class DraftHolder @Inject constructor() {
    private val _draft = MutableStateFlow<PlaylistDraft?>(null)
    val draft: StateFlow<PlaylistDraft?> = _draft.asStateFlow()
    fun set(d: PlaylistDraft?) { _draft.value = d }
    fun consume(): PlaylistDraft? = _draft.value.also { _draft.value = null }
}
