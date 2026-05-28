package com.ytwrap.app.data.repository

import com.ytwrap.app.data.dao.TrackDao
import com.ytwrap.app.data.entity.Track
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val trackDao: TrackDao
) {
    fun observeLikedTracks(): Flow<List<Track>> = trackDao.observeLikedTracks()

    fun observeAllTracks(): Flow<List<Track>> = trackDao.observeAllTracks()

    fun observeTrackById(videoId: String): Flow<Track?> = trackDao.observeTrackById(videoId)

    fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)

    suspend fun getTrackById(videoId: String): Track? = trackDao.getTrackById(videoId)

    suspend fun insertTrack(track: Track) = trackDao.insertTrack(track)

    suspend fun toggleLike(videoId: String) {
        val track = trackDao.getTrackById(videoId)
        if (track != null) {
            trackDao.setLiked(videoId, !track.isLiked)
        }
    }

    suspend fun setLiked(videoId: String, isLiked: Boolean) = trackDao.setLiked(videoId, isLiked)

    suspend fun deleteTrack(track: Track) = trackDao.deleteTrack(track)

    suspend fun updateAddedAt(videoId: String, addedAt: Long) = trackDao.updateAddedAt(videoId, addedAt)

    suspend fun getLikedTracksList(): List<Track> = trackDao.getLikedTracksList()

    fun observeOfflineTracks(): Flow<List<Track>> = trackDao.observeOfflineTracks()

    suspend fun setOffline(videoId: String, isOffline: Boolean, filePath: String?) =
        trackDao.setOffline(videoId, isOffline, filePath)
}
