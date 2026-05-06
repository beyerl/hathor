package com.lenzbeyer.hathor.domain

/**
 * SPEC §8.1: pick a single folder artist for a playlist.
 *  1. If every kept entry's parsed artist is identical → use it.
 *  2. Otherwise → first kept entry's uploader (channel name) — script #2 default.
 */
object FolderArtistResolver {
    data class Entry(val parsedArtist: String, val uploader: String)

    fun resolve(entries: List<Entry>): String {
        if (entries.isEmpty()) return ""
        val artists = entries.map { it.parsedArtist.trim() }.filter { it.isNotEmpty() }.toSet()
        return if (artists.size == 1) artists.first()
        else entries.first().uploader.trim()
    }
}
