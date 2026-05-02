package com.ytwrap.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.ytwrap.app.ui.theme.MiniPlayerGradient
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyGreenDark
import com.ytwrap.app.ui.theme.SpotifyGreenLight
import com.ytwrap.app.ui.theme.SpotifyLikeColor
import com.ytwrap.app.ui.viewmodel.PlayerUiState

/**
 * Collapsed mini-player bar shown at the bottom of the screen.
 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onTap: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    Column {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MiniPlayerGradient)
                .clickable(onClick = onTap)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = SpotifyGreen.copy(alpha = 0.8f)
                    )
                }

                IconButton(onClick = onSkipPrevious) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
        }

        // Green progress indicator
        val progress = if (state.durationMs > 0) {
            state.currentPositionMs.toFloat() / state.durationMs.toFloat()
        } else 0f

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = SpotifyGreen,
            trackColor = Color(0xFF404040),
        )
    }
}

/**
 * Expanded "Now Playing" screen shown as a bottom sheet.
 */
@Composable
fun NowPlayingSheet(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    // Animated background gradient — cycles through mood colors (like HomeScreen)
    val headerColors = remember {
        listOf(
            Color(0xFF1DB954), // green
            Color(0xFF8B5CF6), // purple
            Color(0xFFE67E22), // warm orange
            Color(0xFF14B8A6), // teal
            Color(0xFFE84393), // rose
            Color(0xFF3B82F6), // blue
        )
    }
    var colorIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            colorIndex = (colorIndex + 1) % headerColors.size
        }
    }
    val animatedColor by animateColorAsState(
        targetValue = headerColors[colorIndex].copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "nowPlayingGradient"
    )

    Box(
        modifier = modifier
            .wrapContentHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedColor, SpotifyBlack),
                    startY = 0f,
                    endY = 600f
                )
            )
    ) {

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SpotifyGreen.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Album art with shadow
            Box {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Title + Artist + Like
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = SpotifyGreen
                    )
                }

                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (state.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (state.isLiked) SpotifyLikeColor else Color(0xFFB3B3B3)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seek bar
            var isSeeking by remember { mutableStateOf(false) }
            var seekPosition by remember { mutableFloatStateOf(0f) }

            val currentProgress = if (state.durationMs > 0) {
                state.currentPositionMs.toFloat() / state.durationMs.toFloat()
            } else 0f

            Slider(
                value = if (isSeeking) seekPosition else currentProgress.coerceIn(0f, 1f),
                onValueChange = { value ->
                    isSeeking = true
                    seekPosition = value
                },
                onValueChangeFinished = {
                    onSeek((seekPosition * state.durationMs).toLong())
                    isSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = Color(0xFF535353)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(state.currentPositionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3B3B3)
                )
                Text(
                    text = formatDuration(state.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3B3B3)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Music visualizer above controls
            if (state.isPlaying) {
                MusicVisualizer(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    barCount = 32
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleEnabled) SpotifyGreen else Color(0xFFB3B3B3)
                    )
                }

                // Previous
                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                // Play/Pause (large green circle)
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(68.dp)
                        .background(SpotifyGreen, CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(38.dp),
                        tint = Color.Black
                    )
                }

                // Next
                IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                // Repeat
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = when (state.repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) SpotifyGreen
                        else Color(0xFFB3B3B3)
                    )
                }
            }
        }
    }
}

/**
 * Queuing popup shown above the mini player while a song is being loaded.
 */
@Composable
fun QueuingPopup(
    trackName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "queuing")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        SpotifyGreenDark.copy(alpha = 0.3f),
                        Color(0xFF1A1A2E)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = SpotifyGreen,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Queuing your request",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SpotifyGreen.copy(alpha = dotAlpha)
                )
                Text(
                    text = trackName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun MusicVisualizer(
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    barColor: Color = SpotifyGreen
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    
    // Create animated heights for each bar with different phases
    val barHeights = List(barCount) { index ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800 + (index * 50), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEach { heightAnim ->
            val height by heightAnim
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp * height)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(barColor)
            )
        }
    }
}
