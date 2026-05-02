package com.ytwrap.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.ui.components.AddToPlaylistDialog
import com.ytwrap.app.ui.components.MiniPlayer
import com.ytwrap.app.ui.components.NowPlayingSheet
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyDarkGray
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyTextSecondary
import com.ytwrap.app.ui.viewmodel.LibraryViewModel
import com.ytwrap.app.ui.viewmodel.PlayerViewModel
import com.ytwrap.app.ui.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
    statsViewModel: StatsViewModel
) {
    val playerState by playerViewModel.uiState.collectAsState()
    val playlists by libraryViewModel.playlists.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val snackbarHostState = remember { SnackbarHostState() }

    // Playlist detail navigation
    var openPlaylistId by remember { mutableStateOf<Long?>(null) }

    // Library tab initial sub-tab (0=Liked, 1=Playlists)
    var libraryInitialTab by remember { mutableIntStateOf(0) }

    // Add-to-playlist dialog
    var trackForPlaylistPicker by remember { mutableStateOf<Track?>(null) }

    // Show player errors as a Snackbar
    LaunchedEffect(playerState.error) {
        playerState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            playerViewModel.clearError()
        }
    }

    // Record plays for stats — only once per track change
    var lastRecordedTrackId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(playerState.currentTrack?.videoId) {
        val track = playerState.currentTrack
        if (track != null && track.videoId != lastRecordedTrackId) {
            lastRecordedTrackId = track.videoId
            statsViewModel.recordPlay(
                videoId = track.videoId,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                durationMs = track.durationMs
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                // Mini player above the nav bar
                if (playerState.currentTrack != null) {
                    MiniPlayer(
                        state = playerState,
                        onTap = { showNowPlaying = true },
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onSkipPrevious = { playerViewModel.skipPrevious() },
                        onSkipNext = { playerViewModel.skipNext() }
                    )
                }

                // Bottom navigation with 4 tabs
                NavigationBar(
                    containerColor = SpotifyDarkGray,
                    contentColor = SpotifyTextSecondary
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Home",
                                tint = if (selectedTab == 0) SpotifyGreen else SpotifyTextSecondary
                            )
                        },
                        label = {
                            Text(
                                "Home",
                                color = if (selectedTab == 0) SpotifyGreen else SpotifyTextSecondary
                            )
                        },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = if (selectedTab == 1) SpotifyGreen else SpotifyTextSecondary
                            )
                        },
                        label = {
                            Text(
                                "Search",
                                color = if (selectedTab == 1) SpotifyGreen else SpotifyTextSecondary
                            )
                        },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Filled.LibraryMusic,
                                contentDescription = "Library",
                                tint = if (selectedTab == 2) SpotifyGreen else SpotifyTextSecondary
                            )
                        },
                        label = {
                            Text(
                                "Library",
                                color = if (selectedTab == 2) SpotifyGreen else SpotifyTextSecondary
                            )
                        },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Filled.BarChart,
                                contentDescription = "Stats",
                                tint = if (selectedTab == 3) SpotifyGreen else SpotifyTextSecondary
                            )
                        },
                        label = {
                            Text(
                                "Stats",
                                color = if (selectedTab == 3) SpotifyGreen else SpotifyTextSecondary
                            )
                        },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (openPlaylistId != null) {
                PlaylistDetailScreen(
                    playlistId = openPlaylistId!!,
                    viewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    onTrackClick = { track -> playerViewModel.playTrack(track) },
                    onPlayAll = { tracks -> playerViewModel.playQueue(tracks) },
                    onBack = { openPlaylistId = null }
                )
            } else {
                when (selectedTab) {
                    0 -> HomeScreen(
                        playerViewModel = playerViewModel,
                        libraryViewModel = libraryViewModel,
                        statsViewModel = statsViewModel,
                        onLikedSongsTap = {
                            libraryInitialTab = 0
                            selectedTab = 2
                        },
                        onPlaylistsTap = {
                            libraryInitialTab = 1
                            selectedTab = 2
                        },
                        onStatsTap = { selectedTab = 3 }
                    )
                    1 -> SearchScreen(
                        viewModel = libraryViewModel,
                        onTrackClick = { track -> playerViewModel.playTrack(track) },
                        onLikeToggle = { track -> trackForPlaylistPicker = track }
                    )
                    2 -> LibraryScreen(
                        viewModel = libraryViewModel,
                        onTrackClick = { track -> playerViewModel.playTrack(track) },
                        onTrackClickInQueue = { track, queue ->
                            playerViewModel.playTrackInQueue(track, queue)
                        },
                        onPlaylistClick = { playlistId -> openPlaylistId = playlistId },
                        onLikeToggle = { track -> trackForPlaylistPicker = track },
                        initialTab = libraryInitialTab
                    )
                    3 -> StatsScreen(viewModel = statsViewModel)
                }
            }
        }
    }

    // Now Playing Bottom Sheet — opens at partial (~70%) height, draggable to full
    if (showNowPlaying) {
        ModalBottomSheet(
            onDismissRequest = { showNowPlaying = false },
            sheetState = sheetState,
            containerColor = SpotifyBlack,
            dragHandle = null,
            modifier = Modifier.fillMaxWidth()
        ) {
            NowPlayingSheet(
                state = playerState,
                onPlayPause = { playerViewModel.togglePlayPause() },
                onSkipNext = { playerViewModel.skipNext() },
                onSkipPrevious = { playerViewModel.skipPrevious() },
                onSeek = { playerViewModel.seekTo(it) },
                onToggleShuffle = { playerViewModel.toggleShuffle() },
                onToggleRepeat = { playerViewModel.toggleRepeat() },
                onToggleLike = { playerViewModel.toggleLike() }
            )
        }
    }

    // Add to playlist dialog
    if (trackForPlaylistPicker != null) {
        val track = trackForPlaylistPicker!!
        AddToPlaylistDialog(
            playlists = playlists,
            isLiked = track.isLiked,
            onToggleLike = { libraryViewModel.toggleLike(track) },
            onAddToPlaylist = { playlistId ->
                libraryViewModel.addTrackToPlaylist(playlistId, track)
            },
            onDismiss = { trackForPlaylistPicker = null }
        )
    }
}
