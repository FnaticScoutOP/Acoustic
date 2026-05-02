package com.ytwrap.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ytwrap.app.data.entity.ListeningEvent
import kotlinx.coroutines.flow.Flow

data class TrackPlayCount(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val playCount: Int
)

data class HourlyListening(
    val hour: Int,
    val totalMs: Long
)

@Dao
interface ListeningEventDao {

    @Insert
    suspend fun insert(event: ListeningEvent)

    @Query(
        """SELECT video_id AS videoId, title, artist, thumbnail_url AS thumbnailUrl, COUNT(*) AS playCount
           FROM listening_events
           GROUP BY video_id
           ORDER BY playCount DESC
           LIMIT :limit"""
    )
    fun observeTopTracks(limit: Int = 5): Flow<List<TrackPlayCount>>

    @Query(
        """SELECT video_id AS videoId, title, artist, thumbnail_url AS thumbnailUrl, COUNT(*) AS playCount
           FROM listening_events
           GROUP BY video_id
           ORDER BY playCount DESC
           LIMIT 1"""
    )
    fun observeMostPlayedTrack(): Flow<TrackPlayCount?>

    @Query(
        """SELECT SUM(duration_ms) FROM listening_events
           WHERE played_at >= :startOfDay AND played_at < :endOfDay"""
    )
    fun observeTotalListeningMs(startOfDay: Long, endOfDay: Long): Flow<Long?>

    @Query(
        """SELECT CAST(strftime('%H', datetime(played_at / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS hour,
                  SUM(duration_ms) AS totalMs
           FROM listening_events
           WHERE played_at >= :startOfDay AND played_at < :endOfDay
           GROUP BY hour
           ORDER BY hour"""
    )
    fun observeHourlyListening(startOfDay: Long, endOfDay: Long): Flow<List<HourlyListening>>

    /** Heavy rotation: most played tracks in the last 7 days */
    @Query(
        """SELECT video_id AS videoId, title, artist, thumbnail_url AS thumbnailUrl, COUNT(*) AS playCount
           FROM listening_events
           WHERE played_at >= :sinceMs
           GROUP BY video_id
           ORDER BY playCount DESC
           LIMIT :limit"""
    )
    fun observeHeavyRotation(sinceMs: Long, limit: Int = 8): Flow<List<TrackPlayCount>>

    /** Forgotten gems: tracks not played in at least :thresholdMs milliseconds */
    @Query(
        """SELECT video_id AS videoId, title, artist, thumbnail_url AS thumbnailUrl, COUNT(*) AS playCount
           FROM listening_events
           GROUP BY video_id
           HAVING MAX(played_at) < :thresholdMs
           ORDER BY playCount DESC
           LIMIT :limit"""
    )
    fun observeForgottenGems(thresholdMs: Long, limit: Int = 15): Flow<List<TrackPlayCount>>

    /** Recently added: tracks ordered by most recent first listen */
    @Query(
        """SELECT video_id AS videoId, title, artist, thumbnail_url AS thumbnailUrl, COUNT(*) AS playCount
           FROM listening_events
           GROUP BY video_id
           ORDER BY MIN(played_at) DESC
           LIMIT :limit"""
    )
    fun observeRecentlyAdded(limit: Int = 15): Flow<List<TrackPlayCount>>
}
