package com.ytwrap.app.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
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
import com.ytwrap.app.data.repository.TrackRepository
import com.ytwrap.app.playback.PlaybackService
import com.ytwrap.app.playback.YouTubeExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackRepository: TrackRepository,
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

    /**
     * Play a single track: resolve its stream URL, set metadata, and start playback.
     */
    fun playTrack(track: Track) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuffering = true, error = null) }

            // Persist track in DB
            trackRepository.insertTrack(track)

            // Check that the media controller is connected
            val controller = mediaController
            if (controller == null) {
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        error = "Player not ready yet — please try again"
                    )
                }
                return@launch
            }

            // Resolve stream URL
            val streamResult = youTubeExtractor.extractStream(track.videoId)
            streamResult.onSuccess { result ->
                val mediaItem = MediaItem.Builder()
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

                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()

                // Read persisted like state from DB
                val persisted = trackRepository.getTrackById(track.videoId)
                _uiState.update {
                    it.copy(
                        currentTrack = track,
                        isLiked = persisted?.isLiked ?: track.isLiked,
                        isBuffering = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        error = "Failed to load: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Play a list of tracks starting from a given index.
     */
    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuffering = true, error = null, queue = tracks) }

            val controller = mediaController
            if (controller == null) {
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        error = "Player not ready yet — please try again"
                    )
                }
                return@launch
            }

            val mediaItems = tracks.mapNotNull { track ->
                trackRepository.insertTrack(track)
                val streamResult = youTubeExtractor.extractStream(track.videoId)
                streamResult.getOrNull()?.let { result ->
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

            if (mediaItems.isNotEmpty()) {
                controller.setMediaItems(mediaItems, startIndex, 0L)
                controller.prepare()
                controller.play()
            } else {
                _uiState.update {
                    it.copy(error = "Could not load any tracks")
                }
            }

            _uiState.update { it.copy(isBuffering = false) }
        }
    }

    /**
     * Play a specific track within a queue (e.g. liked songs list).
     * Resolves all tracks and starts from the given index.
     */
    fun playTrackInQueue(track: Track, queue: List<Track>) {
        val index = queue.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
        playQueue(queue, index)
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

    fun skipPrevious() {
        mediaController?.seekToPreviousMediaItem()
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
