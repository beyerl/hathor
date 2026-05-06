package com.lenzbeyer.hathor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lenzbeyer.hathor.domain.TrackStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks WHERE playlistId = :playlistId ORDER BY `index`")
    fun observeForPlaylist(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE playlistId = :playlistId ORDER BY `index`")
    suspend fun forPlaylist(playlistId: String): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(track: TrackEntity)

    @Query("UPDATE tracks SET status = :status, errorMessage = :error, updatedAt = :ts WHERE id = :id")
    suspend fun updateStatus(id: String, status: TrackStatus, error: String?, ts: Long)

    @Query("UPDATE tracks SET mp3Uri = :uri, updatedAt = :ts WHERE id = :id")
    suspend fun setMp3Uri(id: String, uri: String, ts: Long)
}
