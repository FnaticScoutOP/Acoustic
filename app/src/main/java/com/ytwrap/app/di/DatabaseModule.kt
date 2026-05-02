package com.ytwrap.app.di

import android.content.Context
import androidx.room.Room
import com.ytwrap.app.data.dao.ListeningEventDao
import com.ytwrap.app.data.dao.PlaylistDao
import com.ytwrap.app.data.dao.TrackDao
import com.ytwrap.app.data.database.YTWrapDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YTWrapDatabase {
        return Room.databaseBuilder(
            context,
            YTWrapDatabase::class.java,
            "ytwrap_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTrackDao(database: YTWrapDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: YTWrapDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    @Singleton
    fun provideListeningEventDao(database: YTWrapDatabase): ListeningEventDao {
        return database.listeningEventDao()
    }
}
