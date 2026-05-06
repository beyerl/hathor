package com.lenzbeyer.hathor.domain

/**
 * Smart tag parser per SPEC §9.1.
 *
 * Order of operations:
 *  1. yt-dlp metadata short-circuit (artist/track filled by YouTube Music).
 *  2. Title/artist split (hyphen / em-dash, [bracket prefix], pipe-suffix).
 *  3. Fallback: artist = uploader, title = video title.
 *  4. Strip cosmetic suffixes (Official Video, HD, Remastered 2020, …).
 *  5. Extract featured-artist credits → comma-separated artist list, strip from title.
 */
object TagParser {

    data class ParsedTags(val artist: String, val title: String)

    private val splitDash      = Regex("^(.+?)\\s+[-–—]\\s+(.+)$")
    private val splitBracket   = Regex("^\\[(.+?)]\\s*(.+)$")
    private val splitPipe      = Regex("^(.+?)\\s+\\|\\s+(.+)$")

    private val cosmeticBracket = Regex(
        "[\\[(]\\s*(?:" +
            "official\\s+(?:music\\s+)?video|" +
            "official\\s+audio|" +
            "lyric(?:\\s+video)?|" +
            "audio\\s+only|" +
            "hd|hq|4k|" +
            "remaster(?:ed)?(?:\\s+\\d{4})?|" +
            "m/v|mv" +
            ")\\s*[\\])]",
        RegexOption.IGNORE_CASE,
    )

    private val featRegex = Regex(
        "\\s*[\\[(]?\\s*(?:feat\\.?|ft\\.?|featuring)\\s+([^()\\[\\]]+?)\\s*[\\])]?\\s*$",
        RegexOption.IGNORE_CASE,
    )

    private val featSeparators = Regex("\\s*(?:,|&|;| and | x | X )\\s*")

    /** Direct parse from raw video metadata. */
    fun parse(
        rawTitle: String,
        uploader: String,
        ytdlpArtist: String? = null,
        ytdlpTrack:  String? = null,
        stripCosmetic: Boolean = true,
    ): ParsedTags {
        // Short-circuit: YouTube Music tracks already have authoritative artist/track.
        if (!ytdlpArtist.isNullOrBlank() && !ytdlpTrack.isNullOrBlank()) {
            return ParsedTags(ytdlpArtist.trim(), ytdlpTrack.trim())
        }

        val (rawArtist, rawTitleSplit) = split(rawTitle, uploader)
        val cleaned = if (stripCosmetic) stripCosmeticSuffixes(rawTitleSplit) else rawTitleSplit
        val (finalArtist, finalTitle) = extractFeatured(rawArtist, cleaned)
        return ParsedTags(finalArtist.trim(), finalTitle.trim())
    }

    private fun split(title: String, uploader: String): Pair<String, String> {
        splitDash.matchEntire(title.trim())?.groupValues?.let {
            return it[1].trim() to it[2].trim()
        }
        splitBracket.matchEntire(title.trim())?.groupValues?.let {
            return it[1].trim() to it[2].trim()
        }
        splitPipe.matchEntire(title.trim())?.groupValues?.let {
            // pipe convention is "Title | Artist" — note swap
            return it[2].trim() to it[1].trim()
        }
        return uploader.trim() to title.trim()
    }

    private fun stripCosmeticSuffixes(title: String): String {
        var t = title
        var changed = true
        while (changed) {
            val replaced = cosmeticBracket.replace(t, "")
            changed = replaced != t
            t = replaced
        }
        return t.trim()
    }

    /** Extracts feat. clauses from title and folds names into the artist list. */
    private fun extractFeatured(mainArtist: String, title: String): Pair<String, String> {
        val match = featRegex.find(title) ?: return mainArtist to title

        val featBlock = match.groupValues[1].trim()
        val featNames = featSeparators.split(featBlock)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val newTitle = title.removeRange(match.range).trim().trimEnd('-', '–', '—', ',', ' ')
        val combinedArtist = (listOf(mainArtist) + featNames)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")

        return combinedArtist to newTitle
    }
}
