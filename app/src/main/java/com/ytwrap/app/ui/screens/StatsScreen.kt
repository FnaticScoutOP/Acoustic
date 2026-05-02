package com.ytwrap.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ytwrap.app.data.dao.HourlyListening
import com.ytwrap.app.data.dao.TrackPlayCount
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyGray
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyGreenDark
import com.ytwrap.app.ui.theme.SpotifyTextSecondary
import com.ytwrap.app.ui.viewmodel.StatsViewModel

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val todayMs by viewModel.todayListeningMs.collectAsState()
    val hourly by viewModel.hourlyListening.collectAsState()
    val mostPlayed by viewModel.mostPlayedTrack.collectAsState()
    val topTracks by viewModel.topTracks.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Music Stats",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Today's listening time
        TodayListeningCard(todayMs ?: 0L)

        Spacer(modifier = Modifier.height(20.dp))

        // Hourly chart
        Text(
            text = "Today's Activity",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        HourlyChart(hourly)

        Spacer(modifier = Modifier.height(24.dp))

        // Most played track
        Text(
            text = "Most Played Track",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (mostPlayed != null) {
            MostPlayedCard(mostPlayed!!)
        } else {
            EmptyStatsCard("No plays recorded yet")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Top 5 tracks
        Text(
            text = "Top 5 Songs",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (topTracks.isEmpty()) {
            EmptyStatsCard("Start listening to see your top tracks")
        } else {
            topTracks.forEachIndexed { index, track ->
                TopTrackRow(index + 1, track)
                if (index < topTracks.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TodayListeningCard(totalMs: Long) {
    val hours = totalMs / 3_600_000
    val minutes = (totalMs % 3_600_000) / 60_000

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpotifyGray)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Today's Listening",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextSecondary
                )
                Text(
                    text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = SpotifyGreen
                )
            }
        }
    }
}

@Composable
private fun HourlyChart(data: List<HourlyListening>) {
    val maxMs = data.maxOfOrNull { it.totalMs } ?: 1L

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SpotifyGray)
            .padding(16.dp)
    ) {
        if (data.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data for today", color = SpotifyTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                for (hour in 0..23) {
                    val entry = data.find { it.hour == hour }
                    val fraction = if (entry != null) entry.totalMs.toFloat() / maxMs.toFloat() else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((fraction * 80).dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (fraction > 0f) SpotifyGreen else SpotifyGreenDark.copy(alpha = 0.3f)
                                )
                        )
                        if (hour % 6 == 0) {
                            Text(
                                text = "${hour}",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.sp),
                                color = SpotifyTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MostPlayedCard(track: TrackPlayCount) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpotifyGray)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Repeat,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "${track.playCount} plays",
                style = MaterialTheme.typography.labelMedium,
                color = SpotifyGreen
            )
        }
    }
}

@Composable
private fun TopTrackRow(rank: Int, track: TrackPlayCount) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpotifyGray)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SpotifyGreen,
            modifier = Modifier.width(36.dp)
        )
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${track.playCount}×",
            style = MaterialTheme.typography.labelLarge,
            color = SpotifyGreen
        )
    }
}

@Composable
private fun EmptyStatsCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpotifyGray)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = SpotifyGreen.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SpotifyTextSecondary
            )
        }
    }
}
