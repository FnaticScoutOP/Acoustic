package com.ytwrap.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ytwrap.app.data.dao.ListeningEventDao
import com.ytwrap.app.data.dao.PlaylistDao
import com.ytwrap.app.data.dao.TrackDao
import com.ytwrap.app.data.entity.ListeningEvent
import com.ytwrap.app.data.entity.Playlist
import com.ytwrap.app.data.entity.PlaylistTrackCrossRef
import com.ytwrap.app.data.entity.Track

@Database(
    entities = [
        Track::class,
        Playlist::class,
        PlaylistTrackCrossRef::class,
        ListeningEvent::class
    ],
    version = 2,
    exportSchema = false
)
abstract class YTWrapDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun listeningEventDao(): ListeningEventDao
}
