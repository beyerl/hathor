package com.lenzbeyer.hathor.di

import android.content.Context
import androidx.room.Room
import com.lenzbeyer.hathor.data.HathorDatabase
import com.lenzbeyer.hathor.data.PlaylistDao
import com.lenzbeyer.hathor.data.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): HathorDatabase =
        Room.databaseBuilder(ctx, HathorDatabase::class.java, "hathor.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides fun providePlaylistDao(db: HathorDatabase): PlaylistDao = db.playlists()
    @Provides fun provideTrackDao   (db: HathorDatabase): TrackDao    = db.tracks()
}
