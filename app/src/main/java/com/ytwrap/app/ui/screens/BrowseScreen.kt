package com.ytwrap.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.ui.components.NowPlayingIndicator
import com.ytwrap.app.ui.theme.AnimatedGradientBackground
import com.ytwrap.app.ui.theme.SpotifyGray
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyTextSecondary
import com.ytwrap.app.ui.viewmodel.LibraryViewModel
import com.ytwrap.app.ui.viewmodel.PlayerViewModel

@Composable
fun BrowseScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onShowNowPlaying: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val likedTracks by libraryViewModel.likedTracks.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val currentTrackId = playerState.currentTrack?.videoId

    var recommendations by remember { mutableStateOf<List<Track>>(emptyList()) }
    var artistPicks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var favouriteArtistTracks by remember { mutableStateOf<Map<String, List<Track>>>(emptyMap()) }
    var isLoadingRecs by remember { mutableStateOf(false) }
    var isLoadingArtist by remember { mutableStateOf(false) }
    var isLoadingFavArtists by remember { mutableStateOf(false) }

    // Generate recommendations based on liked songs
    LaunchedEffect(likedTracks) {
        if (likedTracks.isNotEmpty()) {
            isLoadingRecs = true
            // Pick a random liked track to base recommendations on
            val seed = likedTracks.random()
            val query = "${seed.artist} ${seed.title.take(20)} mix"
            val result = libraryViewModel.searchForBrowse(query)
            recommendations = result.filter { rec ->
                likedTracks.none { it.videoId == rec.videoId }
            }.take(15)
            isLoadingRecs = false

            // Search for another artist
            isLoadingArtist = true
            val artistSeed = likedTracks.shuffled().firstOrNull { it.artist != seed.artist } ?: seed
            val artistQuery = "${artistSeed.artist} latest songs"
            val artistResult = libraryViewModel.searchForBrowse(artistQuery)
            artistPicks = artistResult.filter { rec ->
                likedTracks.none { it.videoId == rec.videoId } &&
                    recommendations.none { it.videoId == rec.videoId }
            }.take(15)
            isLoadingArtist = false

            // Favourite artists — get unique artists from liked songs and search each
            isLoadingFavArtists = true
            val uniqueArtists = likedTracks.map { it.artist }.distinct().take(5)
            val artistMap = mutableMapOf<String, List<Track>>()
            for (artist in uniqueArtists) {
                val results = libraryViewModel.searchForBrowse("$artist songs")
                val filtered = results.filter { r ->
                    likedTracks.none { it.videoId == r.videoId }
                }.take(10)
                if (filtered.isNotEmpty()) {
                    artistMap[artist] = filtered
                }
            }
            favouriteArtistTracks = artistMap
            isLoadingFavArtists = false
        }
    }

    AnimatedGradientBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Browse",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            if (likedTracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = SpotifyGreen.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Like some songs first!",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "We'll recommend music based on your liked songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpotifyTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Recommendations section
                    item(key = "rec_header") {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Recommend,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Based on Your Likes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    if (isLoadingRecs) {
                        item(key = "rec_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SpotifyGreen)
                            }
                        }
                    } else if (recommendations.isNotEmpty()) {
                        item(key = "rec_row") {
                            BrowseCardRow(
                                tracks = recommendations,
                                onTrackClick = {
                                    if (it.videoId == currentTrackId) onShowNowPlaying()
                                    else playerViewModel.playTrackSmart(it)
                                },
                                currentTrackId = currentTrackId
                            )
                        }
                    }

                    // Artist picks section
                    item(key = "artist_header") {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NewReleases,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Discover New Music",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    if (isLoadingArtist) {
                        item(key = "artist_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SpotifyGreen)
                            }
                        }
                    } else if (artistPicks.isNotEmpty()) {
                        item(key = "artist_row") {
                            BrowseCardRow(
                                tracks = artistPicks,
                                onTrackClick = {
                                    if (it.videoId == currentTrackId) onShowNowPlaying()
                                    else playerViewModel.playTrackSmart(it)
                                },
                                currentTrackId = currentTrackId
                            )
                        }
                    }

                    // Favourite Artists section
                    item(key = "fav_artists_header") {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your Favourite Artists",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    if (isLoadingFavArtists) {
                        item(key = "fav_artists_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SpotifyGreen)
                            }
                        }
                    } else if (favouriteArtistTracks.isNotEmpty()) {
                        favouriteArtistTracks.entries.forEachIndexed { idx, (artist, tracks) ->
                            item(key = "fav_artist_name_$idx") {
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = SpotifyGreen,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                            item(key = "fav_artist_row_$idx") {
                                BrowseCardRow(
                                    tracks = tracks,
                                    onTrackClick = {
                                        if (it.videoId == currentTrackId) onShowNowPlaying()
                                        else playerViewModel.playTrackSmart(it)
                                    },
                                    currentTrackId = currentTrackId
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseCardRow(
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
                    .width(140.dp)
                    .clickable { onTrackClick(track) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(10.dp))
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
                    maxLines = 2,
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
