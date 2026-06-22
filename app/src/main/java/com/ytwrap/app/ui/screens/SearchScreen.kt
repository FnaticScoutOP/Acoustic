package com.ytwrap.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.ui.components.TrackItem
import com.ytwrap.app.ui.theme.AnimatedGradientBackground
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyGray
import com.ytwrap.app.ui.theme.SpotifyLightGray
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyTextSecondary
import com.ytwrap.app.ui.viewmodel.ChannelFilter
import com.ytwrap.app.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: LibraryViewModel,
    onTrackClick: (Track) -> Unit,
    onLikeToggle: (Track) -> Unit,
    onMore: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AnimatedGradientBackground(modifier = modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Search bar with filter icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.searchYouTube(it) },
                placeholder = {
                    Text(
                        "Search or paste YouTube link",
                        color = SpotifyTextSecondary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                },
                leadingIcon = {
                    Icon(
                        if (uiState.isResolvingLink || (uiState.searchQuery.isNotBlank() && viewModel.isYouTubeLink(uiState.searchQuery)))
                            Icons.Filled.Link
                        else Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = if (uiState.isResolvingLink || (uiState.searchQuery.isNotBlank() && viewModel.isYouTubeLink(uiState.searchQuery)))
                            SpotifyGreen else Color.White
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.searchYouTube("")
                            viewModel.clearResolvedLink()
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SpotifyGray,
                    unfocusedContainerColor = SpotifyGray,
                    cursorColor = SpotifyGreen,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Filter icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (uiState.channelFilters.isNotEmpty()) SpotifyGreen else SpotifyGray)
                    .clickable { showFilterSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filters",
                    tint = if (uiState.channelFilters.isNotEmpty()) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                // Badge for active filter count
                if (uiState.channelFilters.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE57373)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${uiState.channelFilters.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active channel filter chips (Jira-style)
        if (uiState.channelFilters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.channelFilters.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpotifyGreen.copy(alpha = 0.15f))
                            .clickable { viewModel.removeChannelFilter(filter) }
                            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filter.thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = filter.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = filter.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = SpotifyGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove ${filter.name}",
                            tint = SpotifyGreen.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                // Clear all button
                TextButton(onClick = { viewModel.clearChannelFilters() }) {
                    Text("Clear all", color = SpotifyTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Resolving link indicator
        if (uiState.isResolvingLink) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Resolving YouTube link...", color = SpotifyTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Resolved link highlight
        if (uiState.resolvedLinkTrack != null && !uiState.isResolvingLink) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Video found from link",
                    color = SpotifyGreen,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        when {
            uiState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = SpotifyGreen)
                        }
                    }
                }
            }
            uiState.searchResults.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No results found",
                            color = SpotifyTextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (uiState.channelFilters.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try removing channel filters",
                                color = SpotifyTextSecondary.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            uiState.searchResults.isEmpty() && uiState.searchQuery.isBlank() -> {
                val recentSearches by viewModel.recentSearches.collectAsState()

                if (recentSearches.isNotEmpty()) {
                    LazyColumn {
                        item(key = "recent_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearRecentSearches() }) {
                                    Text("Clear all", color = SpotifyGreen, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        items(recentSearches, key = { "recent_$it" }) { query ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.searchYouTube(query) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = SpotifyTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = query,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.removeRecentSearch(query) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove",
                                        tint = SpotifyTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty state — prompt
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = SpotifyGreen.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Search for music",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Find tracks on YouTube or paste a link",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpotifyTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the filter icon to search by channel",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpotifyTextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            else -> {
                LazyColumn {
                    items(uiState.searchResults, key = { it.videoId }) { track ->
                        val isResolvedLink = track.videoId == uiState.resolvedLinkTrack?.videoId
                        TrackItem(
                            track = track,
                            onClick = { onTrackClick(track) },
                            onLikeToggle = { onLikeToggle(track) },
                            onMore = { onMore(track) }
                        )
                    }
                }
            }
        }
    }
    }

    // ── Channel Filter Bottom Sheet ──────────────────────────────────────
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showFilterSheet = false
                viewModel.clearChannelSearch()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = SpotifyBlack,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SpotifyLightGray)
                )
            }
        ) {
            ChannelFilterSheet(
                viewModel = viewModel,
                uiState = uiState,
                onDismiss = {
                    showFilterSheet = false
                    viewModel.clearChannelSearch()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChannelFilterSheet(
    viewModel: LibraryViewModel,
    uiState: com.ytwrap.app.ui.viewmodel.LibraryUiState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Filter by Channel",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Add channels to filter search results. Like Jira — search only shows videos from selected channels.",
            style = MaterialTheme.typography.bodySmall,
            color = SpotifyTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current channel filters
        if (uiState.channelFilters.isNotEmpty()) {
            Text(
                "Active Filters",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.channelFilters.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SpotifyGreen.copy(alpha = 0.15f))
                            .clickable { viewModel.removeChannelFilter(filter) }
                            .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filter.thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = filter.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = filter.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpotifyGreen,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = SpotifyGreen.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = SpotifyGray)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Channel search
        Text(
            "Search Channels",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = uiState.channelSearchQuery,
            onValueChange = { viewModel.searchChannels(it) },
            placeholder = {
                Text("Search for a channel name", color = SpotifyTextSecondary, style = MaterialTheme.typography.bodyMedium)
            },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White) },
            trailingIcon = {
                if (uiState.channelSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearChannelSearch() }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SpotifyGray,
                unfocusedContainerColor = SpotifyGray,
                cursorColor = SpotifyGreen,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Channel search results
        when {
            uiState.isSearchingChannels -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(24.dp))
                }
            }
            uiState.channelSearchResults.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(uiState.channelSearchResults, key = { it.url }) { channel ->
                        val isSelected = uiState.channelFilters.any { it.url == channel.url }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isSelected) viewModel.addChannelFilter(channel)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Channel thumbnail
                            if (channel.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = channel.thumbnailUrl,
                                    contentDescription = channel.name,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SpotifyGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = SpotifyTextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = if (isSelected) SpotifyGreen else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (channel.subscriberCount > 0) {
                                    Text(
                                        text = formatSubscribers(channel.subscriberCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SpotifyTextSecondary
                                    )
                                }
                            }

                            if (isSelected) {
                                Text(
                                    "Added",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SpotifyGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            uiState.channelSearchQuery.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No channels found", color = SpotifyTextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Done button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Done", color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatSubscribers(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM subscribers", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK subscribers", count / 1_000.0)
        count > 0 -> "$count subscribers"
        else -> ""
    }
}
