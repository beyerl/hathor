package com.lenzbeyer.hathor.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FilenameSanitizerTest {

    @Test fun `keeps alphanumerics spaces hyphens underscores`() {
        assertEquals(
            "Hello World - Track_01",
            FilenameSanitizer.sanitize("Hello World - Track_01"),
        )
    }

    @Test fun `strips slashes colons and other illegal filesystem chars`() {
        assertEquals(
            "AC DC Highway to Hell",
            FilenameSanitizer.sanitize("AC/DC: Highway to Hell"),
        )
    }

    @Test fun `collapses repeated whitespace`() {
        assertEquals(
            "Spaces between",
            FilenameSanitizer.sanitize("Spaces       between"),
        )
    }

    @Test fun `trims leading dots and surrounding whitespace`() {
        assertEquals(
            "hidden file",
            FilenameSanitizer.sanitize("  ...hidden file  "),
        )
    }

    @Test fun `trackFilename uses NN Artist - Title pattern`() {
        assertEquals(
            "01 Daft Punk - One More Time.mp3",
            FilenameSanitizer.trackFilename(1, "Daft Punk", "One More Time"),
        )
    }

    @Test fun `trackFilename pads the index to two digits`() {
        assertEquals(
            "07 Artist - Title.mp3",
            FilenameSanitizer.trackFilename(7, "Artist", "Title"),
        )
        assertEquals(
            "23 Artist - Title.mp3",
            FilenameSanitizer.trackFilename(23, "Artist", "Title"),
        )
    }

    @Test fun `trackFilename falls back to NN - Title when artist blank`() {
        assertEquals(
            "01 - Just A Title.mp3",
            FilenameSanitizer.trackFilename(1, "", "Just A Title"),
        )
    }

    @Test fun `playlistFolderName follows Folder Artist - Playlist Title`() {
        assertEquals(
            "Daft Punk - Discovery",
            FilenameSanitizer.playlistFolderName("Daft Punk", "Discovery"),
        )
    }
}
