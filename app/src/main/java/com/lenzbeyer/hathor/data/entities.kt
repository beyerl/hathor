package com.lenzbeyer.hathor.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lenzbeyer.hathor.domain.TrackStatus

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,                  // youtube playlist id
    val url: String,
    val title: String,
    val folderArtist: String,
    val album: String,
    val year: String?,
    val genre: String?,
    val rootFolderUri: String,                   // SAF tree URI
    val playlistFolderUri: String,
    val coverUrl: String?,                       // remote URL fetched at job start
    val coverJpgUri: String?,                    // SAF URI of saved folder.jpg, set after fetch
    val createdAt: Long,
    val lastSyncedAt: Long?,
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId")],
)
data class TrackEntity(
    @PrimaryKey val id: String,                  // playlistId + videoId
    val playlistId: String,
    val videoId: String,
    val index: Int,
    val title: String,
    val artist: String,
    val trackTotal: Int,
    val mp3Uri: String?,
    val status: TrackStatus,
    val errorMessage: String?,
    val durationMs: Long?,
    val sourceFormat: String?,
    val updatedAt: Long,
)
