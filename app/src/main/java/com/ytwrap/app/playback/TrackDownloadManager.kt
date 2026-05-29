package com.ytwrap.app.playback

import android.content.Context
import android.os.Environment
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.data.repository.TrackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val videoId: String,
    val progress: Float, // 0f..1f
    val isComplete: Boolean = false,
    val error: String? = null
)

@Singleton
class TrackDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeExtractor: YouTubeExtractor,
    private val trackRepository: TrackRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()

    private val _downloadComplete = MutableStateFlow<String?>(null)
    val downloadComplete: StateFlow<String?> = _downloadComplete.asStateFlow()

    private val _deleteComplete = MutableStateFlow<String?>(null)
    val deleteComplete: StateFlow<String?> = _deleteComplete.asStateFlow()

    private fun getDownloadDir(): File {
        // Prefer external storage (SD card / shared storage), fall back to app-internal
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "music")
        if (!musicDir.exists()) musicDir.mkdirs()
        return musicDir
    }

    suspend fun downloadTrack(track: Track): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Update progress
            _activeDownloads.value = _activeDownloads.value +
                (track.videoId to DownloadProgress(track.videoId, 0f))

            // Resolve stream URL
            val streamResult = youTubeExtractor.extractStream(track.videoId)
            val stream = streamResult.getOrNull()
                ?: return@withContext Result.failure(
                    Exception(streamResult.exceptionOrNull()?.message ?: "Failed to resolve stream")
                )

            // Sanitize filename
            val safeName = track.title.replace(Regex("[^a-zA-Z0-9._\\- ]"), "").take(80)
            val file = File(getDownloadDir(), "${safeName}_${track.videoId}.m4a")

            // Download with progress
            val request = Request.Builder().url(stream.audioStreamUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                updateDownloadError(track.videoId, "HTTP ${response.code}")
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body ?: run {
                updateDownloadError(track.videoId, "Empty response")
                return@withContext Result.failure(Exception("Empty response"))
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            FileOutputStream(file).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                        _activeDownloads.value = _activeDownloads.value +
                            (track.videoId to DownloadProgress(track.videoId, progress))
                    }
                }
            }

            // Mark as offline in DB
            trackRepository.insertTrack(track)
            trackRepository.setOffline(track.videoId, true, file.absolutePath)

            // Update progress to complete
            _activeDownloads.value = _activeDownloads.value +
                (track.videoId to DownloadProgress(track.videoId, 1f, isComplete = true))

            // Notify download complete
            _downloadComplete.value = track.videoId

            // Remove from active downloads after a short delay
            kotlinx.coroutines.delay(2000)
            _activeDownloads.value = _activeDownloads.value - track.videoId
            _downloadComplete.value = null

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            updateDownloadError(track.videoId, e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    suspend fun deleteOfflineTrack(track: Track) = withContext(Dispatchers.IO) {
        track.offlineFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        trackRepository.setOffline(track.videoId, false, null)
        _deleteComplete.value = track.videoId
        kotlinx.coroutines.delay(500)
        _deleteComplete.value = null
    }

    private fun updateDownloadError(videoId: String, error: String) {
        _activeDownloads.value = _activeDownloads.value +
            (videoId to DownloadProgress(videoId, 0f, error = error))
    }
}
