package com.lenzbeyer.hathor.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TagParserTest {

    @Test fun `splits on hyphen with spaces`() {
        val r = TagParser.parse("Daft Punk - One More Time", uploader = "Daft Punk Topic")
        assertEquals("Daft Punk", r.artist)
        assertEquals("One More Time", r.title)
    }

    @Test fun `splits on em-dash`() {
        val r = TagParser.parse("Kraftwerk — Autobahn", uploader = "kraftwerkofficial")
        assertEquals("Kraftwerk", r.artist)
        assertEquals("Autobahn", r.title)
    }

    @Test fun `splits on bracket prefix`() {
        val r = TagParser.parse("[Aphex Twin] Xtal", uploader = "warp")
        assertEquals("Aphex Twin", r.artist)
        assertEquals("Xtal", r.title)
    }

    @Test fun `splits on pipe with title-first ordering`() {
        val r = TagParser.parse("Some Song | Some Artist", uploader = "channel")
        assertEquals("Some Artist", r.artist)
        assertEquals("Some Song", r.title)
    }

    @Test fun `falls back to uploader when no separator matches`() {
        val r = TagParser.parse("Just a Title", uploader = "Channel Name")
        assertEquals("Channel Name", r.artist)
        assertEquals("Just a Title", r.title)
    }

    @Test fun `strips Official Music Video suffix`() {
        val r = TagParser.parse("Beyoncé - Halo (Official Music Video)", uploader = "BeyonceVEVO")
        assertEquals("Beyoncé", r.artist)
        assertEquals("Halo", r.title)
    }

    @Test fun `strips multiple cosmetic brackets`() {
        val r = TagParser.parse(
            "Queen - Bohemian Rhapsody (Official Video) [Remastered 2011]",
            uploader = "QueenOfficial",
        )
        assertEquals("Queen", r.artist)
        assertEquals("Bohemian Rhapsody", r.title)
    }

    @Test fun `extracts feat artist comma-separated and strips from title`() {
        val r = TagParser.parse(
            "Mark Ronson - Uptown Funk (feat. Bruno Mars)",
            uploader = "MarkRonsonVEVO",
        )
        assertEquals("Mark Ronson, Bruno Mars", r.artist)
        assertEquals("Uptown Funk", r.title)
    }

    @Test fun `extracts multiple feat artists with ampersand and comma`() {
        val r = TagParser.parse(
            "Calvin Harris - Feels (feat. Pharrell Williams, Katy Perry & Big Sean)",
            uploader = "CalvinHarrisVEVO",
        )
        assertEquals(
            "Calvin Harris, Pharrell Williams, Katy Perry, Big Sean",
            r.artist,
        )
        assertEquals("Feels", r.title)
    }

    @Test fun `handles ft abbreviation`() {
        val r = TagParser.parse("Drake - Hotline Bling ft. Future", uploader = "drake")
        assertEquals("Drake, Future", r.artist)
        assertEquals("Hotline Bling", r.title)
    }

    @Test fun `short-circuits on yt-dlp metadata when present`() {
        val r = TagParser.parse(
            rawTitle = "noisy raw video title (Official Music Video)",
            uploader = "channel",
            ytdlpArtist = "Authoritative Artist",
            ytdlpTrack = "Authoritative Track",
        )
        assertEquals("Authoritative Artist", r.artist)
        assertEquals("Authoritative Track", r.title)
    }

    @Test fun `does not short-circuit when ytdlp fields are blank`() {
        val r = TagParser.parse(
            rawTitle = "Real Artist - Real Title",
            uploader = "channel",
            ytdlpArtist = "",
            ytdlpTrack = "",
        )
        assertEquals("Real Artist", r.artist)
        assertEquals("Real Title", r.title)
    }

    @Test fun `cosmetic stripping respects stripCosmetic flag`() {
        val r = TagParser.parse(
            "Artist - Song (Official Video)",
            uploader = "channel",
            stripCosmetic = false,
        )
        assertEquals("Artist", r.artist)
        assertEquals("Song (Official Video)", r.title)
    }
}
