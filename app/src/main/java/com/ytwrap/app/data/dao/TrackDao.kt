package com.ytwrap.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ytwrap.app.data.entity.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(track: Track)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Delete
    suspend fun deleteTrack(track: Track)

    @Query("SELECT * FROM tracks WHERE video_id = :videoId")
    suspend fun getTrackById(videoId: String): Track?

    @Query("SELECT * FROM tracks WHERE video_id = :videoId")
    fun observeTrackById(videoId: String): Flow<Track?>

    @Query("SELECT * FROM tracks WHERE is_liked = 1 ORDER BY added_at DESC")
    fun observeLikedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY added_at DESC")
    fun observeAllTracks(): Flow<List<Track>>

    @Query("UPDATE tracks SET is_liked = :isLiked WHERE video_id = :videoId")
    suspend fun setLiked(videoId: String, isLiked: Boolean)

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchTracks(query: String): Flow<List<Track>>

    @Query("UPDATE tracks SET added_at = :addedAt WHERE video_id = :videoId")
    suspend fun updateAddedAt(videoId: String, addedAt: Long)

    @Query("SELECT * FROM tracks WHERE is_liked = 1 ORDER BY added_at DESC")
    suspend fun getLikedTracksList(): List<Track>

    @Query("SELECT * FROM tracks WHERE is_offline = 1 ORDER BY added_at DESC")
    fun observeOfflineTracks(): Flow<List<Track>>

    @Query("UPDATE tracks SET is_offline = :isOffline, offline_file_path = :filePath WHERE video_id = :videoId")
    suspend fun setOffline(videoId: String, isOffline: Boolean, filePath: String?)
}
