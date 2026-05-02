package com.ytwrap.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ytwrap.app.data.entity.Playlist
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.ui.components.NowPlayingIndicator
import com.ytwrap.app.ui.components.TrackItem
import com.ytwrap.app.ui.theme.AnimatedGradientBackground
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyDarkGray
import com.ytwrap.app.ui.theme.SpotifyGray
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyLikeColor
import com.ytwrap.app.ui.theme.SpotifyTextSecondary
import com.ytwrap.app.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onTrackClick: (Track) -> Unit,
    onTrackClickInQueue: (Track, List<Track>) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onLikeToggle: (Track) -> Unit = { viewModel.toggleLike(it) },
    initialTab: Int = 0,
    currentTrackId: String? = null,
    onShowNowPlaying: () -> Unit = {},
    currentPlaylistId: Long? = null,
    modifier: Modifier = Modifier
) {
    val likedTracks by viewModel.likedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }

    AnimatedGradientBackground(modifier = modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            // Show '+' only on Playlists tab
            if (selectedTab == 1) {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create Playlist",
                        tint = Color.White
                    )
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = SpotifyGreen
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(
                    "Liked",
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = if (selectedTab == 0) SpotifyGreen else SpotifyTextSecondary
                )
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(
                    "Playlists",
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = if (selectedTab == 1) SpotifyGreen else SpotifyTextSecondary
                )
            }
        }

        // Content
        when (selectedTab) {
            0 -> LikedTracksTab(
                tracks = likedTracks,
                onTrackClick = { track ->
                    if (track.videoId == currentTrackId) onShowNowPlaying()
                    else onTrackClickInQueue(track, likedTracks)
                },
                onLikeToggle = onLikeToggle,
                currentTrackId = currentTrackId
            )
            1 -> PlaylistsTab(
                playlists = playlists,
                onPlaylistClick = onPlaylistClick,
                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                onEditPlaylist = { playlistToEdit = it },
                currentPlaylistId = currentPlaylistId
            )
        }
    }
    }

    // Create Playlist Bottom Sheet
    if (showCreateDialog) {
        PlaylistBottomSheet(
            title = "Create Playlist",
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc ->
                viewModel.createPlaylist(name, desc)
                showCreateDialog = false
            }
        )
    }

    // Edit Playlist Bottom Sheet
    if (playlistToEdit != null) {
        val pl = playlistToEdit!!
        PlaylistBottomSheet(
            title = "Edit Playlist",
            initialName = pl.name,
            initialDescription = pl.description,
            onDismiss = { playlistToEdit = null },
            onConfirm = { name, desc ->
                viewModel.updatePlaylist(pl.copy(name = name, description = desc))
                playlistToEdit = null
            }
        )
    }
}

@Composable
private fun LikedTracksTab(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onLikeToggle: (Track) -> Unit,
    currentTrackId: String? = null
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = SpotifyLikeColor.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No liked tracks yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SpotifyTextSecondary
                )
            }
        }
    } else {
        LazyColumn {
            items(tracks, key = { it.videoId }) { track ->
                TrackItem(
                    track = track,
                    onClick = { onTrackClick(track) },
                    onLikeToggle = { onLikeToggle(track) },
                    isCurrentlyPlaying = track.videoId == currentTrackId
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onEditPlaylist: (Playlist) -> Unit = {},
    currentPlaylistId: Long? = null
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = SpotifyGreen.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No playlists yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SpotifyTextSecondary
                )
            }
        }
    } else {
        LazyColumn {
            items(playlists, key = { it.playlistId }) { playlist ->
                val isPlaying = playlist.playlistId == currentPlaylistId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isPlaying) Modifier.background(Color.White.copy(alpha = 0.08f))
                            else Modifier
                        )
                        .clickable { onPlaylistClick(playlist.playlistId) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = SpotifyGreen
                        )
                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                NowPlayingIndicator(
                                    barWidth = 3.dp,
                                    maxHeight = 16.dp,
                                    gap = 2.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (isPlaying) SpotifyGreen else Color.White
                        )
                        if (playlist.description.isNotBlank()) {
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = SpotifyTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = { onEditPlaylist(playlist) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit playlist",
                            tint = SpotifyTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { onDeletePlaylist(playlist) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete playlist",
                            tint = SpotifyTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider(color = SpotifyGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistBottomSheet(
    title: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF282828),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = SpotifyTextSecondary)
                }
                TextButton(
                    onClick = { if (name.isNotBlank()) onConfirm(name.trim(), description.trim()) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save", color = SpotifyGreen)
                }
            }
        }
    }
}
