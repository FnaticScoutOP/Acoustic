package com.ytwrap.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytwrap.app.data.entity.Playlist
import com.ytwrap.app.data.entity.PlaylistWithTracks
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.data.repository.PlaylistRepository
import com.ytwrap.app.data.repository.TrackRepository
import com.ytwrap.app.playback.YouTubeExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
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
    @ApplicationContext private val context: Context,
    private val trackRepository: TrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val youTubeExtractor: YouTubeExtractor
) : ViewModel() {

    private val searchPrefs = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    init {
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        val saved = searchPrefs.getString("searches", "") ?: ""
        _recentSearches.value = if (saved.isBlank()) emptyList()
            else saved.split("\u001F").filter { it.isNotBlank() }.take(10)
    }

    private fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(10)
        _recentSearches.value = limited
        searchPrefs.edit().putString("searches", limited.joinToString("\u001F")).apply()
    }

    fun removeRecentSearch(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        _recentSearches.value = current
        searchPrefs.edit().putString("searches", current.joinToString("\u001F")).apply()
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        searchPrefs.edit().remove("searches").apply()
    }

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val likedTracks: StateFlow<List<Track>> = trackRepository.observeLikedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.observeAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistsWithTracks: StateFlow<List<PlaylistWithTracks>> =
        playlistRepository.observeAllPlaylistsWithTracks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeOrderedTracksForPlaylist(playlistId: Long): Flow<List<Track>> =
        playlistRepository.observeTracksForPlaylistOrdered(playlistId)

    fun searchYouTube(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        saveRecentSearch(query)
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

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.updatePlaylist(playlist)
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

    suspend fun searchForBrowse(query: String): List<Track> {
        val result = youTubeExtractor.search(query, 15)
        return result.getOrDefault(emptyList())
    }

    fun reorderTracksInPlaylist(playlistId: Long, videoIds: List<String>) {
        viewModelScope.launch {
            playlistRepository.reorderTracks(playlistId, videoIds)
        }
    }

    fun reorderLikedTracks(tracks: List<Track>) {
        viewModelScope.launch {
            val baseTime = System.currentTimeMillis()
            tracks.forEachIndexed { index, track ->
                trackRepository.updateAddedAt(track.videoId, baseTime - index)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
