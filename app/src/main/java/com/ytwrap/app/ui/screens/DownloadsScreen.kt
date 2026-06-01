package com.ytwrap.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.ui.components.NowPlayingIndicator
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyGray
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyTextSecondary
import com.ytwrap.app.ui.viewmodel.LibraryViewModel
import com.ytwrap.app.ui.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onShowNowPlaying: () -> Unit = {},
    currentTrackId: String? = null
) {
    val offlineTracks by libraryViewModel.offlineTracks.collectAsState()
    var isReordering by remember { mutableStateOf(false) }
    var deleteConfirmTrack by remember { mutableStateOf<Track?>(null) }

    val reorderableList = remember { mutableStateListOf<Track>() }

    Scaffold(
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Downloads",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "${offlineTracks.size} songs • ${formatTotalSize(offlineTracks)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotifyTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isReordering) {
                            libraryViewModel.reorderLikedTracks(reorderableList.toList())
                            isReordering = false
                        } else {
                            reorderableList.clear()
                            reorderableList.addAll(offlineTracks)
                            isReordering = true
                        }
                    }) {
                        Icon(
                            Icons.Filled.Reorder,
                            contentDescription = if (isReordering) "Done reordering" else "Reorder",
                            tint = if (isReordering) SpotifyGreen else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { paddingValues ->
        if (offlineTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.DownloadDone,
                        contentDescription = null,
                        tint = SpotifyGreen.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No downloaded songs",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Download songs to play offline",
                        color = SpotifyTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (isReordering) {
            ReorderableDownloadList(
                tracks = reorderableList,
                onMove = { from, to ->
                    val item = reorderableList.removeAt(from)
                    reorderableList.add(to, item)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Play all header
                item(key = "play_all") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (offlineTracks.isNotEmpty()) {
                                    playerViewModel.playQueue(offlineTracks)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SpotifyGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play all", tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Play all", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(offlineTracks, key = { it.videoId }) { track ->
                    DownloadTrackItem(
                        track = track,
                        isCurrentlyPlaying = track.videoId == currentTrackId,
                        onClick = {
                            if (track.videoId == currentTrackId) onShowNowPlaying()
                            else playerViewModel.playTrackInQueue(track, offlineTracks)
                        },
                        onDelete = { deleteConfirmTrack = track }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (deleteConfirmTrack != null) {
        val track = deleteConfirmTrack!!
        AlertDialog(
            onDismissRequest = { deleteConfirmTrack = null },
            title = { Text("Delete download?", color = Color.White) },
            text = {
                Text(
                    "\"${track.title}\" will be removed from your device. The file will be deleted.",
                    color = SpotifyTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    libraryViewModel.deleteOfflineTrack(track)
                    deleteConfirmTrack = null
                }) {
                    Text("Delete", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTrack = null }) {
                    Text("Cancel", color = SpotifyGreen)
                }
            },
            containerColor = SpotifyGray,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ReorderableDownloadList(
    tracks: MutableList<Track>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val itemHeight = 72.dp
    val shakeAngle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        repeat(3) {
            shakeAngle.animateTo(2.5f, animationSpec = tween(50))
            shakeAngle.animateTo(-2.5f, animationSpec = tween(50))
        }
        shakeAngle.animateTo(0f, animationSpec = tween(50))
    }

    LazyColumn(modifier = modifier) {
        itemsIndexed(tracks, key = { _, track -> track.videoId }) { index, track ->
            val elevation by animateDpAsState(
                if (draggedIndex == index) 8.dp else 0.dp,
                label = "downloadReorderElevation"
            )
            val fileSize = track.offlineFilePath?.let {
                val file = java.io.File(it)
                if (file.exists()) file.length() else 0L
            } ?: 0L

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
                if (track.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DownloadDone, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${track.artist} • ${formatFileSize(fileSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = SpotifyTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadTrackItem(
    track: Track,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = false
) {
    val fileSize = track.offlineFilePath?.let {
        val file = java.io.File(it)
        if (file.exists()) file.length() else 0L
    } ?: 0L

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isCurrentlyPlaying)
                    Modifier.background(Color.White.copy(alpha = 0.08f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDragHandle) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                tint = SpotifyTextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )
        }

        // Thumbnail
        Box(contentAlignment = Alignment.Center) {
            if (track.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SpotifyGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(28.dp))
                }
            }
            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    NowPlayingIndicator(barWidth = 3.dp, maxHeight = 18.dp, gap = 2.dp)
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentlyPlaying) SpotifyGreen else Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DownloadDone,
                    contentDescription = "Downloaded",
                    tint = SpotifyGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${track.artist} • ${formatFileSize(fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = SpotifyTextSecondary
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = SpotifyTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatTotalSize(tracks: List<Track>): String {
    val totalBytes = tracks.sumOf { track ->
        track.offlineFilePath?.let {
            val file = java.io.File(it)
            if (file.exists()) file.length() else 0L
        } ?: 0L
    }
    return formatFileSize(totalBytes)
}
