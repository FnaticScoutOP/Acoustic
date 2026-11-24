package com.ytwrap.app.ui.screens

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytwrap.app.data.dao.TrackPlayCount
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
import com.ytwrap.app.ui.viewmodel.StatsViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
    statsViewModel: StatsViewModel,
    onLikedSongsTap: () -> Unit,
    onPlaylistsTap: () -> Unit,
    onStatsTap: () -> Unit,
    onBrowseTap: () -> Unit = {},
    onDownloadsTap: () -> Unit = {},
    onCloudTap: () -> Unit = {},
    onShowNowPlaying: () -> Unit = {},
    onTrackMore: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playerState by playerViewModel.uiState.collectAsState()
    val recentlyPlayed by playerViewModel.recentlyPlayed.collectAsState()
    val heavyRotation by statsViewModel.heavyRotation.collectAsState()
    val forgottenGems by statsViewModel.forgottenGems.collectAsState()
    val recentlyAdded by statsViewModel.recentlyAdded.collectAsState()
    val greeting = getGreeting()

    AnimatedGradientBackground(modifier = modifier) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Greeting header
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        run {
            // Quick Start — Resume card
            val lastTrack = playerState.currentTrack ?: recentlyPlayed.firstOrNull()
            if (lastTrack != null) {
                item(key = "quickstart") {
                    // SectionTitle("Quick Start")
                    ResumeCard(
                        track = lastTrack,
                        isPlaying = playerState.isPlaying && playerState.currentTrack?.videoId == lastTrack.videoId,
                        onPlayPause = {
                            if (playerState.currentTrack?.videoId == lastTrack.videoId) {
                                playerViewModel.togglePlayPause()
                            } else {
                                playerViewModel.playTrack(lastTrack)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Connected device info
            item(key = "connected_device") {
                ConnectedDeviceCard()
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Category Chips
            item(key = "browse") {
                SectionTitle("Browse")
                Spacer(modifier = Modifier.height(8.dp))
                CategoryChipsGrid(
                    onLikedSongsTap = onLikedSongsTap,
                    onPlaylistsTap = onPlaylistsTap,
                    onStatsTap = onStatsTap,
                    onBrowseTap = onBrowseTap,
                    onDownloadsTap = onDownloadsTap,
                    onCloudTap = onCloudTap
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Recently Played (in-memory)
            if (recentlyPlayed.isNotEmpty()) {
                item(key = "recently_played") {
                    SectionTitle("Recently Played")
                    Spacer(modifier = Modifier.height(8.dp))
                    TrackCardRow(
                        tracks = recentlyPlayed.take(15),
                        onTrackClick = {
                            if (it.videoId == playerState.currentTrack?.videoId) onShowNowPlaying()
                            else playerViewModel.playTrackSmart(it)
                        },
                        currentTrackId = playerState.currentTrack?.videoId
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Heavy Rotation (last 7 days)
            if (heavyRotation.isNotEmpty()) {
                item(key = "heavy_rotation") {
                    SectionTitle("Heavy Rotation")
                    Spacer(modifier = Modifier.height(8.dp))
                    PlayCountCardRow(
                        items = heavyRotation,
                        onItemClick = { item -> playerViewModel.playTrackFromStats(item) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Recently Added
            if (recentlyAdded.isNotEmpty()) {
                item(key = "recently_added") {
                    SectionTitle("Recently Added")
                    Spacer(modifier = Modifier.height(8.dp))
                    PlayCountCardRow(
                        items = recentlyAdded,
                        onItemClick = { item -> playerViewModel.playTrackFromStats(item) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Forgotten Gems
            if (forgottenGems.isNotEmpty()) {
                item(key = "forgotten_gems") {
                    SectionTitle("Forgotten Gems")
                    Spacer(modifier = Modifier.height(8.dp))
                    PlayCountCardRow(
                        items = forgottenGems,
                        onItemClick = { item -> playerViewModel.playTrackFromStats(item) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Empty state
            if (recentlyPlayed.isEmpty() && heavyRotation.isEmpty() && lastTrack == null) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SpotifyGreen.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Start playing music!", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Search for songs to get started", color = SpotifyTextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
    } // AnimatedGradientBackground
}

// ── Helpers ─────────────────────────────────────────────────────────────

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        hour < 21 -> "Good Evening"
        else -> "Good Night"
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun ResumeCard(track: Track, isPlaying: Boolean, onPlayPause: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpotifyGray)
            .clickable(onClick = onPlayPause)
            .padding(12.dp),
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
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SpotifyGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun CategoryChipsGrid(
    onLikedSongsTap: () -> Unit,
    onPlaylistsTap: () -> Unit,
    onStatsTap: () -> Unit,
    onBrowseTap: () -> Unit = {},
    onDownloadsTap: () -> Unit = {},
    onCloudTap: () -> Unit = {}
) {
    val chips = listOf(
        Triple(Icons.Filled.Favorite as ImageVector, "Liked Songs", onLikedSongsTap),
        Triple(Icons.AutoMirrored.Filled.QueueMusic as ImageVector, "Playlists", onPlaylistsTap),
        Triple(Icons.Filled.Star as ImageVector, "Top Songs", onStatsTap),
        Triple(Icons.Filled.MusicNote as ImageVector, "Browse", onBrowseTap),
        Triple(Icons.Filled.DownloadDone as ImageVector, "Downloads", onDownloadsTap),
        Triple(Icons.Filled.Cloud as ImageVector, "Cloud", onCloudTap)
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        chips.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (icon, label, action) ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyGray)
                            .clickable(onClick = action)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                    }
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TrackCardRow(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    currentTrackId: String? = null
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tracks, key = { it.videoId }) { track ->
            val isPlaying = track.videoId == currentTrackId
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onTrackClick(track) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            NowPlayingIndicator(
                                barWidth = 5.dp,
                                maxHeight = 28.dp,
                                gap = 3.dp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isPlaying) SpotifyGreen else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlayCountCardRow(items: List<TrackPlayCount>, onItemClick: (TrackPlayCount) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.videoId }) { item ->
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onItemClick(item) }
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.artist} \u2022 ${item.playCount}\u00d7",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ConnectedDeviceCard() {
    val context = LocalContext.current
    var deviceName by remember { mutableStateOf("This device") }
    var deviceIcon by remember { mutableStateOf<ImageVector>(Icons.Filled.PhoneAndroid) }
    var batteryLevel by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            // Find the active audio output device (prefer Bluetooth > wired > speaker)
            val activeDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            } ?: devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            } ?: devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }

            when (activeDevice?.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    deviceName = activeDevice.productName?.toString()?.ifBlank { "Bluetooth device" } ?: "Bluetooth device"
                    deviceIcon = Icons.Filled.Bluetooth

                    // Try to get battery from BluetoothManager
                    try {
                        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                        val adapter = btManager?.adapter
                        if (adapter != null) {
                            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                                    try {
                                        val btDevices = proxy.connectedDevices
                                        for (btDev in btDevices) {
                                            // Match the active audio device
                                            if (btDev.address == activeDevice?.address) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    // Android 13+: Use public API
                                                    try {
                                                        val a2dp = proxy as? android.bluetooth.BluetoothA2dp
                                                        if (a2dp != null) {
                                                            val batteryLevelMethod = android.bluetooth.BluetoothDevice::class.java.getMethod(
                                                                "getBatteryLevel",
                                                                android.bluetooth.BluetoothA2dp::class.java
                                                            )
                                                            val level = batteryLevelMethod.invoke(btDev, a2dp) as? Int
                                                            if (level != null && level >= 0) {
                                                                batteryLevel = level
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        // Fallback to reflection for hidden API
                                                        try {
                                                            val batteryLevelMethod = btDev.javaClass.getMethod("getBatteryLevel")
                                                            val level = batteryLevelMethod.invoke(btDev) as? Int
                                                            if (level != null && level >= 0) {
                                                                batteryLevel = level
                                                            }
                                                        } catch (_: Exception) { }
                                                    }
                                                } else {
                                                    // Android <13: Use reflection for hidden API
                                                    try {
                                                        val batteryLevelMethod = btDev.javaClass.getMethod("getBatteryLevel")
                                                        val level = batteryLevelMethod.invoke(btDev) as? Int
                                                        if (level != null && level >= 0) {
                                                            batteryLevel = level
                                                        }
                                                    } catch (_: Exception) { }
                                                }
                                            }
                                        }
                                    } catch (_: SecurityException) { }
                                    adapter.closeProfileProxy(profile, proxy)
                                }
                                override fun onServiceDisconnected(profile: Int) {}
                            }, BluetoothProfile.A2DP)
                        }
                    } catch (_: Exception) { }
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    deviceName = activeDevice.productName?.toString()?.ifBlank { "Wired headphones" } ?: "Wired headphones"
                    deviceIcon = Icons.Filled.Headphones
                }
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                    deviceName = "Phone speaker"
                    deviceIcon = Icons.Filled.Speaker
                    // Get phone battery level
                    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                }
                else -> {
                    deviceName = "This device"
                    deviceIcon = Icons.Filled.PhoneAndroid
                    // Get phone battery level
                    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                }
            }
        } catch (_: Exception) {
            deviceName = "This device"
        }
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(SpotifyGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = deviceIcon,
            contentDescription = "Device",
            tint = SpotifyGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Listening on",
                style = MaterialTheme.typography.labelSmall,
                color = SpotifyTextSecondary
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (batteryLevel != null) {
            Spacer(modifier = Modifier.width(8.dp))
            val batIcon = when {
                batteryLevel!! >= 90 -> Icons.Filled.BatteryFull
                batteryLevel!! >= 60 -> Icons.Filled.Battery6Bar
                batteryLevel!! >= 40 -> Icons.Filled.Battery4Bar
                batteryLevel!! >= 20 -> Icons.Filled.Battery2Bar
                else -> Icons.Filled.Battery0Bar
            }
            Icon(
                imageVector = batIcon,
                contentDescription = "Battery",
                tint = if (batteryLevel!! <= 20) Color(0xFFE57373) else SpotifyGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${batteryLevel}%",
                style = MaterialTheme.typography.labelSmall,
                color = SpotifyTextSecondary
            )
        }
    }
}
