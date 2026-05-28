package com.ytwrap.app.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.data.repository.PlaylistRepository
import com.ytwrap.app.data.repository.TrackRepository
import com.ytwrap.app.playback.PlaybackService
import com.ytwrap.app.playback.YouTubeExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isLiked: Boolean = false,
    val error: String? = null,
    val queuingTrackName: String? = null,
    val currentPlaylistId: Long? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackRepository: TrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val youTubeExtractor: YouTubeExtractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // In-memory recently played list (lost on app restart — as requested)
    private val _recentlyPlayed = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = _recentlyPlayed.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.let {
                    if (it.isDone && !it.isCancelled) it.get() else null
                }
                mediaController?.addListener(playerListener)
                startPositionUpdates()
            },
            MoreExecutors.directExecutor()
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update {
                it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val videoId = mediaItem?.mediaId ?: return
            viewModelScope.launch {
                val track = trackRepository.getTrackById(videoId)
                if (track != null) {
                    // Add to in-memory recently played (max 30, no duplicates)
                    _recentlyPlayed.update { list ->
                        (listOf(track) + list.filter { it.videoId != track.videoId }).take(30)
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        currentTrack = track,
                        isLiked = track?.isLiked ?: false,
                        durationMs = mediaController?.duration ?: 0L
                    )
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "ExoPlayer error: code=${error.errorCode}, message=${error.message}", error)
            _uiState.update {
                it.copy(
                    error = "Playback error: ${error.message}",
                    isBuffering = false,
                    queuingTrackName = null
                )
            }
        }
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    _uiState.update {
                        it.copy(
                            currentPositionMs = controller.currentPosition,
                            durationMs = controller.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500L)
            }
        }
    }

    // Refresh current track data from database to get latest isOffline status
    fun refreshCurrentTrackFromDb() {
        viewModelScope.launch {
            val current = _uiState.value.currentTrack
            if (current != null) {
                val dbTrack = trackRepository.getTrackById(current.videoId)
                if (dbTrack != null && dbTrack.isOffline != current.isOffline) {
                    _uiState.update { it.copy(currentTrack = dbTrack) }
                }
            }
        }
    }

    /**
     * Play a single track: resolve its stream URL, set metadata, and start playback.
     */
    fun playTrack(track: Track) {
        viewModelScope.launch {
            Log.d(TAG, "playTrack called: videoId=${track.videoId}, title=${track.title}")
            _uiState.update { it.copy(isBuffering = true, error = null, queuingTrackName = track.title) }

            val controller = mediaController
            if (controller == null) {
                Log.e(TAG, "playTrack: mediaController is NULL")
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        error = "Player not ready yet — please try again",
                        queuingTrackName = null
                    )
                }
                return@launch
            }

            val mediaItem = resolveTrackToMediaItem(track)
            if (mediaItem != null) {
                Log.d(TAG, "playTrack: resolved mediaItem URI=${mediaItem.localConfiguration?.uri}")
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
                Log.d(TAG, "playTrack: controller.play() called, state=${controller.playbackState}")

                // Use persisted track from DB to get latest isOffline/isLiked state
                val persisted = trackRepository.getTrackById(track.videoId)
                val updatedTrack = persisted ?: track
                _uiState.update {
                    it.copy(
                        currentTrack = updatedTrack,
                        isLiked = updatedTrack.isLiked,
                        isBuffering = false,
                        queuingTrackName = null
                    )
                }
            } else {
                Log.e(TAG, "playTrack: resolveTrackToMediaItem returned null for ${track.videoId}")
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        error = "Failed to load track",
                        queuingTrackName = null
                    )
                }
            }
        }
    }

    private suspend fun resolveTrackToMediaItem(track: Track): MediaItem? {
        Log.d(TAG, "resolveTrack: start videoId=${track.videoId}")
        trackRepository.insertTrack(track)

        // Check if track is available offline first
        val persisted = trackRepository.getTrackById(track.videoId)
        Log.d(TAG, "resolveTrack: persisted=${persisted != null}, isOffline=${persisted?.isOffline}, path=${persisted?.offlineFilePath}")
        if (persisted != null && persisted.isOffline && persisted.offlineFilePath != null) {
            val file = java.io.File(persisted.offlineFilePath)
            Log.d(TAG, "resolveTrack: offline file exists=${file.exists()}, size=${if (file.exists()) file.length() else 0}")
            if (file.exists()) {
                Log.d(TAG, "resolveTrack: using offline file ${file.absolutePath}")
                val fileUri = Uri.fromFile(file)
                return MediaItem.Builder()
                    .setMediaId(track.videoId)
                    .setUri(fileUri)
                    .setRequestMetadata(
                        MediaItem.RequestMetadata.Builder()
                            .setMediaUri(fileUri)
                            .build()
                    )
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle("Acoustic")
                            .setArtworkUri(Uri.parse(track.thumbnailUrl))
                            .build()
                    )
                    .build()
            }
        }

        Log.d(TAG, "resolveTrack: extracting stream from YouTube for ${track.videoId}")
        val streamResult = withTimeoutOrNull(15_000L) {
            youTubeExtractor.extractStream(track.videoId)
        }
        if (streamResult == null) {
            Log.e(TAG, "resolveTrack: TIMEOUT after 15s for ${track.videoId}")
            return null
        }
        streamResult.onFailure { e ->
            Log.e(TAG, "resolveTrack: extraction FAILED for ${track.videoId}: ${e.javaClass.simpleName}: ${e.message}")
        }
        return streamResult.getOrNull()?.let { result ->
            Log.d(TAG, "resolveTrack: got stream URL (len=${result.audioStreamUrl.length}) for ${track.videoId}")
            MediaItem.Builder()
                .setMediaId(track.videoId)
                .setUri(result.audioStreamUrl)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(Uri.parse(result.audioStreamUrl))
                        .build()
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle("Acoustic")
                        .setArtworkUri(Uri.parse(result.thumbnailUrl))
                        .build()
                )
                .build()
        }
    }

    /**
     * Play a list of tracks starting from a given index.
     * Resolves the target track first for instant playback, then resolves
     * remaining tracks in parallel in the background.
     */
    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuffering = true, error = null, queue = tracks) }

            val controller = mediaController
            if (controller == null) {
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        error = "Player not ready yet — please try again",
                        queuingTrackName = null
                    )
                }
                return@launch
            }

            // Resolve the target track first so playback starts immediately
            val targetTrack = tracks.getOrNull(startIndex)
            val targetItem = targetTrack?.let { resolveTrackToMediaItem(it) }

            if (targetItem != null) {
                controller.setMediaItem(targetItem)
                controller.prepare()
                controller.play()

                val persisted = trackRepository.getTrackById(targetTrack.videoId)
                val updatedTrack = persisted ?: targetTrack
                _uiState.update {
                    it.copy(
                        currentTrack = updatedTrack,
                        isLiked = updatedTrack.isLiked,
                        isBuffering = false,
                        queuingTrackName = null
                    )
                }

                // Resolve remaining tracks in background
                if (tracks.size > 1) {
                    viewModelScope.launch {
                        val remainingItems = withTimeoutOrNull(90_000L) {
                            supervisorScope {
                                tracks.mapIndexedNotNull { index, track ->
                                    if (index == startIndex) null
                                    else async { resolveTrackToMediaItem(track) }
                                }.awaitAll().filterNotNull()
                            }
                        } ?: emptyList()

                        if (remainingItems.isNotEmpty()) {
                            // Build full queue: items before startIndex + target + items after
                            val beforeTarget = remainingItems.take(startIndex)
                            val afterTarget = remainingItems.drop(startIndex)
                            val allItems = beforeTarget + listOf(targetItem) + afterTarget
                            val newStartIndex = beforeTarget.size
                            val currentPos = controller.currentPosition
                            controller.setMediaItems(allItems, newStartIndex, currentPos)
                        }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = "Could not load track",
                        isBuffering = false,
                        queuingTrackName = null
                    )
                }
            }
        }
    }

    /**
     * Play a specific track within a queue (e.g. liked songs list).
     * Resolves all tracks and starts from the given index.
     */
    fun playTrackInQueue(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuffering = true, error = null, queuingTrackName = track.title) }
            val index = queue.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
            playQueue(queue, index)
        }
    }

    /**
     * Remove a track from the current active queue by videoId.
     */
    fun removeFromQueue(videoId: String) {
        val controller = mediaController ?: return
        for (i in 0 until controller.mediaItemCount) {
            if (controller.getMediaItemAt(i).mediaId == videoId) {
                controller.removeMediaItem(i)
                break
            }
        }
        _uiState.update { state ->
            state.copy(queue = state.queue.filter { it.videoId != videoId })
        }
    }

    /**
     * Play a track from stats data (TrackPlayCount → Track lookup → play).
     */
    fun playTrackFromStats(item: com.ytwrap.app.data.dao.TrackPlayCount) {
        viewModelScope.launch {
            val track = trackRepository.getTrackById(item.videoId)
            if (track != null) {
                playTrack(track)
            } else {
                // Fallback: construct a minimal Track from stats data
                playTrack(
                    Track(
                        videoId = item.videoId,
                        title = item.title,
                        artist = item.artist,
                        thumbnailUrl = item.thumbnailUrl,
                        durationMs = 0L
                    )
                )
            }
        }
    }

    /**
     * Smart play: find the track in liked songs or playlists.
     * Pick the list with the fewest songs and play that list on loop
     * starting from the tapped track. Falls back to single track play.
     */
    fun playTrackSmart(track: Track) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuffering = true, error = null, queuingTrackName = track.title) }
            trackRepository.insertTrack(track)

            // Collect liked songs
            val likedTracks = trackRepository.getLikedTracksList()
            // Collect all playlists with tracks
            val allPlaylists = playlistRepository.getAllPlaylistsWithTracksList()

            // Build candidate lists that contain this track
            data class CandidateList(val tracks: List<Track>, val name: String, val playlistId: Long?)
            val candidates = mutableListOf<CandidateList>()

            if (likedTracks.any { it.videoId == track.videoId }) {
                candidates.add(CandidateList(likedTracks, "Liked Songs", null))
            }
            for (pw in allPlaylists) {
                if (pw.tracks.any { it.videoId == track.videoId }) {
                    candidates.add(CandidateList(pw.tracks, pw.playlist.name, pw.playlist.playlistId))
                }
            }

            if (candidates.isNotEmpty()) {
                // Pick the list with fewest songs
                val best = candidates.minByOrNull { it.tracks.size }!!
                val index = best.tracks.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
                // Play queue and set repeat all
                _uiState.update { it.copy(queuingTrackName = null, currentPlaylistId = best.playlistId) }
                playQueue(best.tracks, index)
                // Set repeat mode to ALL after a short delay to ensure controller is ready
                viewModelScope.launch {
                    delay(500)
                    mediaController?.let { it.repeatMode = Player.REPEAT_MODE_ALL }
                    _uiState.update { it.copy(repeatMode = Player.REPEAT_MODE_ALL) }
                }
            } else {
                // Not found in any list, play single track
                _uiState.update { it.copy(queuingTrackName = null, currentPlaylistId = null) }
                playTrack(track)
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    private var lastSkipPreviousTime = 0L

    fun skipPrevious() {
        val controller = mediaController ?: return
        val now = System.currentTimeMillis()
        val timeSinceLastPress = now - lastSkipPreviousTime
        lastSkipPreviousTime = now

        if (timeSinceLastPress < 5000L) {
            // Second press within 5s — go to previous track
            controller.seekToPreviousMediaItem()
        } else {
            // First press — restart current song from beginning
            controller.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun toggleRepeat() {
        mediaController?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun toggleLike() {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            trackRepository.toggleLike(track.videoId)
            _uiState.update { it.copy(isLiked = !it.isLiked) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
