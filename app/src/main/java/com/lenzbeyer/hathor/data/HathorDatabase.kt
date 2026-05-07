package com.lenzbeyer.hathor.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.lenzbeyer.hathor.domain.TrackStatus

class TrackStatusConverters {
    @TypeConverter fun toDb(s: TrackStatus): String = s.name
    @TypeConverter fun fromDb(s: String): TrackStatus = TrackStatus.valueOf(s)
}

@Database(
    entities = [PlaylistEntity::class, TrackEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(TrackStatusConverters::class)
abstract class HathorDatabase : RoomDatabase() {
    abstract fun playlists(): PlaylistDao
    abstract fun tracks(): TrackDao
}
