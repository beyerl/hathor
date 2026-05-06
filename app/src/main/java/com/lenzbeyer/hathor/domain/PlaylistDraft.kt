package com.lenzbeyer.hathor.domain

/** Pre-download model populated from yt-dlp enumeration; user edits this before queueing. */
data class PlaylistDraft(
    val sourceUrl: String,
    val youtubePlaylistId: String,
    val title: String,
    val folderArtist: String,
    val album: String,
    val year: String?,        // playlist-level — applies to every track (SPEC §9.3)
    val genre: String?,       // playlist-level — applies to every track
    val coverUrl: String?,    // remote URL; downloaded into folder.jpg at job start
    val tracks: List<TrackDraft>,
)

data class TrackDraft(
    val videoId: String,
    val originalIndex: Int,
    val artist: String,
    val title: String,
    val durationMs: Long?,
    val isSkipped: Boolean = false,
)
