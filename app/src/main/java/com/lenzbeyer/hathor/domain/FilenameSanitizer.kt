package com.lenzbeyer.hathor.domain

/**
 * SPEC §8.2: keep alphanumerics + space, hyphen, underscore.
 * Plus Android extensions: collapse repeated whitespace, trim leading dots.
 */
object FilenameSanitizer {
    private val multiWhitespace = Regex("\\s+")

    fun sanitize(name: String): String {
        val filtered = name.filter { it.isLetterOrDigit() || it in " -_" }
        return multiWhitespace.replace(filtered, " ")
            .trimStart('.')
            .trim()
    }

    /** SPEC §8.3 — `NN Artist - Title.mp3` (or `NN - Title.mp3` if artist blank). */
    fun trackFilename(index: Int, artist: String, title: String): String {
        val a = sanitize(artist)
        val t = sanitize(title)
        return if (a.isBlank()) "%02d - %s.mp3".format(index, t)
        else "%02d %s - %s.mp3".format(index, a, t)
    }

    /** SPEC §8.1 — `<Folder Artist> - <Playlist Title>`. */
    fun playlistFolderName(folderArtist: String, playlistTitle: String): String =
        "${sanitize(folderArtist)} - ${sanitize(playlistTitle)}"
}
