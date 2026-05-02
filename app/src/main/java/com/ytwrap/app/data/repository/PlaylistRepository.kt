package com.ytwrap.app.data.repository

import com.ytwrap.app.data.dao.PlaylistDao
import com.ytwrap.app.data.entity.Playlist
import com.ytwrap.app.data.entity.PlaylistTrackCrossRef
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.data.entity.PlaylistWithTracks
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    fun observeAllPlaylists(): Flow<List<Playlist>> = playlistDao.observeAllPlaylists()

    fun observePlaylistWithTracks(playlistId: Long): Flow<PlaylistWithTracks?> =
        playlistDao.observePlaylistWithTracks(playlistId)

    fun observeTracksForPlaylistOrdered(playlistId: Long): Flow<List<Track>> =
        playlistDao.observeTracksForPlaylistOrdered(playlistId)

    fun observeAllPlaylistsWithTracks(): Flow<List<PlaylistWithTracks>> =
        playlistDao.observeAllPlaylistsWithTracks()

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return playlistDao.insertPlaylist(
            Playlist(name = name, description = description)
        )
    }

    suspend fun updatePlaylist(playlist: Playlist) = playlistDao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)

    suspend fun addTrackToPlaylist(playlistId: Long, videoId: String) {
        val count = playlistDao.getTrackCountForPlaylist(playlistId)
        playlistDao.insertPlaylistTrackCrossRef(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                videoId = videoId,
                position = count
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, videoId: String) =
        playlistDao.removeTrackFromPlaylist(playlistId, videoId)

    suspend fun reorderTracks(playlistId: Long, videoIds: List<String>) {
        videoIds.forEachIndexed { index, videoId ->
            playlistDao.updateTrackPosition(playlistId, videoId, index)
        }
    }

    suspend fun getAllPlaylistsWithTracksList(): List<PlaylistWithTracks> =
        playlistDao.getAllPlaylistsWithTracksList()
}
