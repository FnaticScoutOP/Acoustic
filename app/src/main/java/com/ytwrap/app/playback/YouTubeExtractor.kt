package com.ytwrap.app.playback

import android.net.Uri
import com.ytwrap.app.data.entity.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts raw audio stream URLs from YouTube using NewPipe Extractor.
 *
 * WHY THIS APPROACH:
 * The official YouTube Data API v3 does NOT provide direct media stream URLs,
 * and its Terms of Service prohibit background playback for non-Premium users.
 *
 * NewPipe Extractor is a FOSS library that parses YouTube pages directly and
 * extracts the raw adaptive stream URLs (DASH audio-only streams). This is the
 * same approach used by NewPipe, LibreTube, and Piped — all well-known FOSS
 * Android YouTube clients.
 *
 * LEGAL NOTE: This approach is in a legal gray area. It does not use an API key,
 * but it does scrape YouTube. Use at your own discretion for personal/educational
 * projects. Do NOT distribute on Google Play without legal counsel.
 *
 * ALTERNATIVES:
 *  - Piped API (piped.video): A proxy that wraps NewPipe Extractor in a REST API
 *  - Invidious API: Another privacy-respecting YouTube proxy
 *  - youtube-dl / yt-dlp: Command-line tools (not ideal for Android)
 */
@Singleton
class YouTubeExtractor @Inject constructor() {

    init {
        // Initialize NewPipe Extractor with a dummy downloader
        // In production, you'd implement a proper Downloader using OkHttp
        if (!isInitialized) {
            NewPipe.init(DownloaderImpl.getInstance())
            isInitialized = true
        }
    }

    companion object {
        private var isInitialized = false
        private const val YT_BASE_URL = "https://www.youtube.com/watch?v="
        private const val MAX_RETRIES = 3

        fun extractVideoId(url: String): String? {
            val patterns = listOf(
                Regex("""(?:v=|youtu\.be/)([a-zA-Z0-9_-]{11})"""),
                Regex("""^([a-zA-Z0-9_-]{11})$""")
            )
            for (pattern in patterns) {
                val match = pattern.find(url)
                if (match != null) return match.groupValues[1]
            }
            return null
        }
    }

    /**
     * Data class holding the resolved stream information.
     */
    data class StreamResult(
        val audioStreamUrl: String,
        val title: String,
        val artist: String,
        val thumbnailUrl: String,
        val durationMs: Long
    )

    /**
     * Extracts the best audio stream URL for a given YouTube video ID.
     * Must be called from a coroutine (IO dispatcher recommended).
     */
    suspend fun extractStream(videoId: String): Result<StreamResult> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val url = "$YT_BASE_URL$videoId"
                val service = NewPipe.getService(ServiceList.YouTube.serviceId)
                val extractor = service.getStreamExtractor(url)
                extractor.fetchPage()

                // Log available streams for debugging
                val audioStreams = extractor.audioStreams
                val videoStreams = extractor.videoStreams
                val videoOnlyStreams = extractor.videoOnlyStreams
                android.util.Log.d("YouTubeExtractor",
                    "Attempt ${attempt + 1} for $videoId: " +
                    "audioStreams=${audioStreams?.size ?: 0}, " +
                    "videoStreams=${videoStreams?.size ?: 0}, " +
                    "videoOnlyStreams=${videoOnlyStreams?.size ?: 0}")

                // Priority: audio-only > video-only (m4a audio from video) > video streams (last resort)
                val bestAudio = audioStreams
                    ?.sortedByDescending { it.averageBitrate }
                    ?.firstOrNull()

                val bestVideoOnly = videoOnlyStreams?.firstOrNull()

                val streamUrl = bestAudio?.content
                    ?: bestVideoOnly?.content
                    ?: videoStreams
                        ?.firstOrNull()
                        ?.content

                if (streamUrl == null) {
                    android.util.Log.w("YouTubeExtractor",
                        "Attempt ${attempt + 1}: no usable streams for $videoId, retrying...")
                    lastException = Exception("No audio or video streams found for $videoId")
                    if (attempt < MAX_RETRIES - 1) {
                        kotlinx.coroutines.delay(1000L * (attempt + 1))
                    }
                    return@repeat
                }

                val thumbnailUrl = extractor.thumbnails
                    ?.maxByOrNull { it.height * it.width }
                    ?.url ?: ""

                val uploaderName = extractor.uploaderName ?: "Unknown Artist"
                val title = extractor.name ?: "Unknown Title"
                val durationMs = extractor.length * 1000L

                android.util.Log.d("YouTubeExtractor",
                    "Success for $videoId: streamUrl length=${streamUrl.length}, " +
                    "streamType=${when {
                        bestAudio != null -> "audio"
                        bestVideoOnly != null -> "videoOnly"
                        else -> "videoWithAudio"
                    }}")

                return@withContext Result.success(
                    StreamResult(
                        audioStreamUrl = streamUrl,
                        title = title,
                        artist = uploaderName,
                        thumbnailUrl = thumbnailUrl,
                        durationMs = durationMs
                    )
                )
            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("YouTubeExtractor", "Attempt ${attempt + 1} failed for $videoId: ${e.javaClass.simpleName}: ${e.message}")
                // Brief delay before retry
                if (attempt < MAX_RETRIES - 1) {
                    kotlinx.coroutines.delay(500L * (attempt + 1))
                }
            }
        }
        android.util.Log.e("YouTubeExtractor", "All retries exhausted for $videoId", lastException)
        Result.failure(lastException ?: Exception("Unknown extraction error"))
    }

    /**
     * Searches YouTube and returns a list of results.
     */
    suspend fun search(query: String, maxResults: Int = 20): Result<List<Track>> =
        withContext(Dispatchers.IO) {
            try {
                val service = NewPipe.getService(ServiceList.YouTube.serviceId)
                val searchExtractor = service.getSearchExtractor(query)
                searchExtractor.fetchPage()

                val items = searchExtractor.initialPage.items
                    .filterIsInstance<StreamInfoItem>()
                    .take(maxResults)
                    .map { item ->
                        val videoId = extractVideoId(item.url) ?: ""
                        Track(
                            videoId = videoId,
                            title = item.name ?: "Unknown",
                            artist = item.uploaderName ?: "Unknown",
                            thumbnailUrl = item.thumbnails
                                ?.maxByOrNull { it.height * it.width }
                                ?.url ?: "",
                            durationMs = item.duration * 1000L
                        )
                    }
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Converts a Track to a resolved stream URL for playback.
     */
    suspend fun resolveTrackToUri(track: Track): Uri? {
        val result = extractStream(track.videoId)
        return result.getOrNull()?.let { Uri.parse(it.audioStreamUrl) }
    }

    /**
     * Data class representing a YouTube channel for filtering.
     */
    data class ChannelResult(
        val name: String,
        val url: String,
        val thumbnailUrl: String,
        val subscriberCount: Long = -1,
        val description: String = ""
    )

    /**
     * Searches YouTube for channels matching the query.
     */
    suspend fun searchChannels(query: String, maxResults: Int = 10): Result<List<ChannelResult>> =
        withContext(Dispatchers.IO) {
            try {
                val service = NewPipe.getService(ServiceList.YouTube.serviceId)
                val searchExtractor = service.getSearchExtractor(
                    query,
                    listOf("channels"),
                    ""
                )
                searchExtractor.fetchPage()

                val channels = searchExtractor.initialPage.items
                    .filterIsInstance<ChannelInfoItem>()
                    .take(maxResults)
                    .map { item ->
                        ChannelResult(
                            name = item.name ?: "Unknown Channel",
                            url = item.url ?: "",
                            thumbnailUrl = item.thumbnails
                                ?.maxByOrNull { it.height * it.width }
                                ?.url ?: "",
                            subscriberCount = item.subscriberCount,
                            description = item.description ?: ""
                        )
                    }
                Result.success(channels)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Gets videos from a specific channel URL by fetching the channel's Videos tab.
     */
    suspend fun getChannelVideos(channelUrl: String, maxResults: Int = 30): Result<List<Track>> =
        withContext(Dispatchers.IO) {
            try {
                val service = NewPipe.getService(ServiceList.YouTube.serviceId)
                val channelExtractor = service.getChannelExtractor(channelUrl)
                channelExtractor.fetchPage()

                val channelName = channelExtractor.name ?: "Unknown"

                // NewPipe v0.26+: channel content is organized in tabs
                val tabs = channelExtractor.tabs
                val videosTab = tabs.firstOrNull { tab ->
                    tab.contentFilters.any { it.contains("video", ignoreCase = true) }
                } ?: tabs.firstOrNull()

                if (videosTab != null) {
                    val tabExtractor = service.getChannelTabExtractor(videosTab)
                    tabExtractor.fetchPage()

                    val items = tabExtractor.initialPage.items
                        .filterIsInstance<StreamInfoItem>()
                        .take(maxResults)
                        .map { item ->
                            val videoId = extractVideoId(item.url) ?: ""
                            Track(
                                videoId = videoId,
                                title = item.name ?: "Unknown",
                                artist = item.uploaderName ?: channelName,
                                thumbnailUrl = item.thumbnails
                                    ?.maxByOrNull { it.height * it.width }
                                    ?.url ?: "",
                                durationMs = item.duration * 1000L
                            )
                        }
                    Result.success(items)
                } else {
                    Result.success(emptyList())
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Resolves a YouTube URL (video link) to a Track.
     * Supports youtube.com/watch?v=, youtu.be/, youtube.com/shorts/, etc.
     */
    suspend fun resolveVideoLink(url: String): Result<Track> = withContext(Dispatchers.IO) {
        try {
            val videoId = extractVideoId(url)
                ?: return@withContext Result.failure(Exception("Invalid YouTube URL"))

            val streamResult = extractStream(videoId)
            streamResult.map { sr ->
                Track(
                    videoId = videoId,
                    title = sr.title,
                    artist = sr.artist,
                    thumbnailUrl = sr.thumbnailUrl,
                    durationMs = sr.durationMs
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a string looks like a YouTube URL.
     */
    fun isYouTubeLink(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.contains("youtube.com/") ||
                trimmed.contains("youtu.be/") ||
                trimmed.contains("youtube.com/shorts/") ||
                trimmed.contains("music.youtube.com/")
    }
}
