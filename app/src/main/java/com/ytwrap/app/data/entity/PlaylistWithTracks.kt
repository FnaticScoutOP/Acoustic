package com.ytwrap.app.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Represents a [Playlist] together with all its associated [Track]s.
 * Room resolves this via the [PlaylistTrackCrossRef] junction table.
 */
data class PlaylistWithTracks(
    @Embedded
    val playlist: Playlist,

    @Relation(
        parentColumn = "playlist_id",
        entityColumn = "video_id",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlist_id",
            entityColumn = "video_id"
        )
    )
    val tracks: List<Track>
)
