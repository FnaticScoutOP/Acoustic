package com.ytwrap.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.ui.components.NowPlayingIndicator
import com.ytwrap.app.ui.components.TrackItem
import com.ytwrap.app.ui.theme.AnimatedGradientBackground
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyGray
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyTextSecondary
import com.ytwrap.app.ui.viewmodel.LibraryViewModel
import com.ytwrap.app.ui.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onTrackClick: (Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onBack: () -> Unit,
    currentTrackId: String? = null,
    onShowNowPlaying: () -> Unit = {},
    isPlaylistPlaying: Boolean = false,
    isReordering: Boolean = false,
    onReorderingChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playlistsWithTracks by viewModel.playlistsWithTracks.collectAsState()
    val playlistWithTracks = playlistsWithTracks.find { it.playlist.playlistId == playlistId }
    val tracks by viewModel.observeOrderedTracksForPlaylist(playlistId).collectAsState(initial = emptyList<Track>())

    // Track options dialog state
    var trackForOptions by remember { mutableStateOf<Track?>(null) }
    val reorderableTracks = remember { mutableStateListOf<Track>() }

    AnimatedGradientBackground(modifier = modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (isReordering) {
                    reorderableTracks.clear()
                    onReorderingChange(false)
                } else {
                    onBack()
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = playlistWithTracks?.playlist?.name ?: "Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        if (playlistWithTracks == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Playlist not found", color = SpotifyTextSecondary)
            }
        } else {
            // Playlist info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpotifyGray)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpotifyGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        if (isPlaylistPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                NowPlayingIndicator(
                                    barWidth = 4.dp,
                                    maxHeight = 20.dp,
                                    gap = 3.dp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlistWithTracks.playlist.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (playlistWithTracks.playlist.description.isNotBlank()) {
                            Text(
                                text = playlistWithTracks.playlist.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = SpotifyTextSecondary
                            )
                        }
                        Text(
                            text = "${tracks.size} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotifyTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Play All button
            if (tracks.isNotEmpty()) {
                Button(
                    onClick = { onPlayAll(tracks) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play All", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Reorder toggle
            if (tracks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = {
                        if (isReordering) {
                            viewModel.reorderTracksInPlaylist(
                                playlistId,
                                reorderableTracks.map { it.videoId }
                            )
                            onReorderingChange(false)
                        } else {
                            reorderableTracks.clear()
                            reorderableTracks.addAll(tracks)
                            onReorderingChange(true)
                        }
                    }) {
                        Text(
                            text = if (isReordering) "Done" else "Reorder",
                            color = SpotifyGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Track list
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs in this playlist yet",
                        color = SpotifyTextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (isReordering) {
                ReorderableTrackList(
                    tracks = reorderableTracks,
                    onMove = { from, to ->
                        val item = reorderableTracks.removeAt(from)
                        reorderableTracks.add(to, item)
                    }
                )
            } else {
                LazyColumn {
                    items(items = tracks, key = { it.videoId }) { track: Track ->
                        TrackItem(
                            track = track,
                            onClick = {
                                if (track.videoId == currentTrackId) onShowNowPlaying()
                                else onTrackClick(track)
                            },
                            onMore = { trackForOptions = track },
                            isCurrentlyPlaying = track.videoId == currentTrackId
                        )
                    }
                }
            }
        }
    }
    }

    // Track options bottom sheet
    if (trackForOptions != null) {
        val track = trackForOptions!!
        ModalBottomSheet(
            onDismissRequest = { trackForOptions = null },
            containerColor = Color(0xFF282828),
            tonalElevation = 0.dp
        ) {
            Column {
                // Track title
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                HorizontalDivider(color = SpotifyGray)

                // Remove from playlist option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.removeTrackFromPlaylist(playlistId, track.videoId)
                            playerViewModel.removeFromQueue(track.videoId)
                            trackForOptions = null
                        }
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.RemoveCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Remove from playlist", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ReorderableTrackList(
    tracks: MutableList<Track>,
    onMove: (Int, Int) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val itemHeight = 72.dp

    // Shake animation on enter
    val shakeAngle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // Quick jerk: rotate back and forth a few times
        repeat(3) {
            shakeAngle.animateTo(2.5f, animationSpec = tween(50))
            shakeAngle.animateTo(-2.5f, animationSpec = tween(50))
        }
        shakeAngle.animateTo(0f, animationSpec = tween(50))
    }

    LazyColumn {
        itemsIndexed(tracks, key = { _, track -> track.videoId }) { index, track ->
            val elevation by animateDpAsState(
                if (draggedIndex == index) 8.dp else 0.dp,
                label = "elevation"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationZ = if (draggedIndex == null) shakeAngle.value else 0f }
                    .shadow(elevation)
                    .background(if (draggedIndex == index) SpotifyGray else Color.Transparent)
                    .pointerInput(tracks.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = index
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y
                                val currentIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                val itemHeightPx = itemHeight.toPx()
                                if (dragOffset >= itemHeightPx) {
                                    val targetIndex = (currentIndex + 1).coerceIn(0, tracks.size - 1)
                                    if (targetIndex != currentIndex) {
                                        onMove(currentIndex, targetIndex)
                                        draggedIndex = targetIndex
                                    }
                                    dragOffset = 0f
                                } else if (dragOffset <= -itemHeightPx) {
                                    val targetIndex = (currentIndex - 1).coerceIn(0, tracks.size - 1)
                                    if (targetIndex != currentIndex) {
                                        onMove(currentIndex, targetIndex)
                                        draggedIndex = targetIndex
                                    }
                                    dragOffset = 0f
                                }
                            },
                            onDragEnd = {
                                draggedIndex = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggedIndex = null
                                dragOffset = 0f
                            }
                        )
                    }
                    .offset {
                        IntOffset(
                            x = 0,
                            y = if (draggedIndex == index) dragOffset.roundToInt() else 0
                        )
                    }
                    .then(if (draggedIndex == index) Modifier.zIndex(1f) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = SpotifyTextSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                coil.compose.AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = SpotifyTextSecondary
                    )
                }
            }
        }
    }
}
