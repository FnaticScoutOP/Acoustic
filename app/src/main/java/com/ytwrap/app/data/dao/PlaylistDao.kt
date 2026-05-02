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
import com.ytwrap.app.data.entity.Track
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

    @Query(
        "SELECT t.* FROM tracks t INNER JOIN playlist_track_cross_ref p ON t.video_id = p.video_id WHERE p.playlist_id = :playlistId ORDER BY p.position ASC"
    )
    fun observeTracksForPlaylistOrdered(playlistId: Long): Flow<List<Track>>

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

    @Query("UPDATE playlist_track_cross_ref SET position = :position WHERE playlist_id = :playlistId AND video_id = :videoId")
    suspend fun updateTrackPosition(playlistId: Long, videoId: String, position: Int)

    @Query("SELECT video_id FROM playlist_track_cross_ref WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun getTrackIdsForPlaylist(playlistId: Long): List<String>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updated_at DESC")
    suspend fun getAllPlaylistsWithTracksList(): List<PlaylistWithTracks>
}
