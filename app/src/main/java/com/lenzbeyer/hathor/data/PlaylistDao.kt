package com.lenzbeyer.hathor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun byId(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: String): Flow<PlaylistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET lastSyncedAt = :ts WHERE id = :id")
    suspend fun touchSyncTimestamp(id: String, ts: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: String)
}
