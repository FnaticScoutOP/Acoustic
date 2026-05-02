package com.ytwrap.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ytwrap.app.data.entity.Playlist
import com.ytwrap.app.data.entity.PlaylistTrackCrossRef
import com.ytwrap.app.data.entity.PlaylistWithTracks
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlists ORDER BY updated_at DESC")
    fun observeAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE playlist_id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlist_id = :playlistId")
    fun observePlaylistWithTracks(playlistId: Long): Flow<PlaylistWithTracks?>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updated_at DESC")
    fun observeAllPlaylistsWithTracks(): Flow<List<PlaylistWithTracks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrackCrossRef(crossRef: PlaylistTrackCrossRef)

    @Delete
    suspend fun deletePlaylistTrackCrossRef(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlist_id = :playlistId AND video_id = :videoId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, videoId: String)

    @Query("SELECT COUNT(*) FROM playlist_track_cross_ref WHERE playlist_id = :playlistId")
    suspend fun getTrackCountForPlaylist(playlistId: Long): Int
}
