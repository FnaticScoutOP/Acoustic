package com.ytwrap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytwrap.app.data.entity.Playlist
import com.ytwrap.app.data.entity.PlaylistWithTracks
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.data.repository.PlaylistRepository
import com.ytwrap.app.data.repository.TrackRepository
import com.ytwrap.app.playback.YouTubeExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val searchQuery: String = "",
    val searchResults: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val youTubeExtractor: YouTubeExtractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val likedTracks: StateFlow<List<Track>> = trackRepository.observeLikedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.observeAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistsWithTracks: StateFlow<List<PlaylistWithTracks>> =
        playlistRepository.observeAllPlaylistsWithTracks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun searchYouTube(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            youTubeExtractor.search(query)
                .onSuccess { results ->
                    // Merge like state from local DB so hearts reflect persisted state
                    val merged = results.map { track ->
                        val existing = trackRepository.getTrackById(track.videoId)
                        if (existing != null) track.copy(isLiked = existing.isLiked) else track
                    }
                    _uiState.update {
                        it.copy(searchResults = merged, isSearching = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.message, isSearching = false)
                    }
                }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            trackRepository.insertTrack(track)
            playlistRepository.addTrackToPlaylist(playlistId, track.videoId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, videoId: String) {
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(playlistId, videoId)
        }
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            trackRepository.insertTrack(track)
            trackRepository.toggleLike(track.videoId)
            // Update search results in-place so the heart icon reflects the new state
            _uiState.update { state ->
                state.copy(
                    searchResults = state.searchResults.map {
                        if (it.videoId == track.videoId) it.copy(isLiked = !track.isLiked) else it
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
