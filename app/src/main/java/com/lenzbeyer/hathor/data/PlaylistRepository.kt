package com.lenzbeyer.hathor.data

import com.lenzbeyer.hathor.domain.TrackStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlists: PlaylistDao,
    private val tracks: TrackDao,
) {

    fun observeAllPlaylists(): Flow<List<PlaylistEntity>> = playlists.observeAll()
    fun observePlaylist(id: String): Flow<PlaylistEntity?> = playlists.observeById(id)
    fun observeTracks(playlistId: String): Flow<List<TrackEntity>> =
        tracks.observeForPlaylist(playlistId)

    suspend fun playlist(id: String): PlaylistEntity? = playlists.byId(id)
    suspend fun upsertPlaylist(p: PlaylistEntity) = playlists.upsert(p)
    suspend fun upsertTracks(t: List<TrackEntity>) = tracks.upsertAll(t)

    suspend fun markStatus(trackId: String, status: TrackStatus, error: String? = null) {
        tracks.updateStatus(trackId, status, error, System.currentTimeMillis())
    }

    suspend fun setMp3Uri(trackId: String, uri: String) {
        tracks.setMp3Uri(trackId, uri, System.currentTimeMillis())
    }

    suspend fun touchSync(playlistId: String) {
        playlists.touchSyncTimestamp(playlistId, System.currentTimeMillis())
    }
}
