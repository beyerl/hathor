package com.lenzbeyer.hathor.media

import android.content.Context
import android.net.Uri
import com.lenzbeyer.hathor.data.PlaylistRepository
import com.lenzbeyer.hathor.data.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.images.StandardArtwork
import org.jaudiotagger.tag.reference.PictureTypes

@Singleton
class ID3Tagger @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: PlaylistRepository,
) {
    suspend fun applyTags(mp3: File, track: TrackEntity) = withContext(Dispatchers.IO) {
        val playlist = repo.playlist(track.playlistId) ?: error("playlist missing for track ${track.id}")

        val audioFile = AudioFileIO.read(mp3)
        val tag = (audioFile.tagOrCreateAndSetDefault as? ID3v23Tag) ?: ID3v23Tag()

        tag.setField(FieldKey.TITLE, track.title)
        tag.setField(FieldKey.ARTIST, track.artist)
        tag.setField(FieldKey.ALBUM, playlist.album)
        playlist.year?.let  { tag.setField(FieldKey.YEAR, it) }
        playlist.genre?.let { tag.setField(FieldKey.GENRE, it) }
        tag.setField(FieldKey.TRACK, "${track.index}/${track.trackTotal}")

        val coverBytes = readCoverBytes(playlist.coverJpgUri)
        if (coverBytes != null && coverBytes.isNotEmpty()) {
            tag.deleteArtworkField()
            val artwork = StandardArtwork().apply {
                binaryData = coverBytes
                mimeType = "image/jpeg"
                description = ""
                pictureType = PictureTypes.DEFAULT_ID
            }
            tag.setField(artwork)
        }

        audioFile.tag = tag
        AudioFileIO.write(audioFile)
    }

    private fun readCoverBytes(uri: String?): ByteArray? {
        if (uri.isNullOrBlank()) return null
        return runCatching {
            ctx.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
        }.getOrNull()
    }
}
