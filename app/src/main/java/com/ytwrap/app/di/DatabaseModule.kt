package com.ytwrap.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `listening_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`video_id` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`artist` TEXT NOT NULL, " +
                "`thumbnail_url` TEXT NOT NULL, " +
                "`duration_ms` INTEGER NOT NULL DEFAULT 0, " +
                "`played_at` INTEGER NOT NULL DEFAULT 0)"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tracks` ADD COLUMN `is_offline` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tracks` ADD COLUMN `offline_file_path` TEXT DEFAULT NULL")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YTWrapDatabase {
        return Room.databaseBuilder(
            context,
            YTWrapDatabase::class.java,
            "ytwrap_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
