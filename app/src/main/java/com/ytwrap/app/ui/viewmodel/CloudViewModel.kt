package com.ytwrap.app.ui.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytwrap.app.cloud.CloudDownloadProgress
import com.ytwrap.app.cloud.CloudUploadProgress
import com.ytwrap.app.cloud.MegaApiClient
import com.ytwrap.app.cloud.MegaNode
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.data.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CloudUiState(
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val deleteConfirmNode: MegaNode? = null,
    val message: String? = null
)

@HiltViewModel
class CloudViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val megaApiClient: MegaApiClient,
    private val trackRepository: TrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudUiState())
    val uiState: StateFlow<CloudUiState> = _uiState.asStateFlow()

    val isLoggedIn = megaApiClient.isLoggedIn
    val loggedInEmail = megaApiClient.loggedInEmail
    val cloudFiles = megaApiClient.cloudFiles
    val isLoading = megaApiClient.isLoading
    val uploadProgress = megaApiClient.uploadProgress
    val downloadProgress = megaApiClient.downloadProgress

    val offlineTracks: StateFlow<List<Track>> = trackRepository.observeOfflineTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            val result = megaApiClient.login(email, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoggingIn = false, loginError = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoggingIn = false, loginError = e.message) }
            }
        }
    }

    fun logout() {
        megaApiClient.logout()
    }

    fun refreshFiles() {
        viewModelScope.launch {
            megaApiClient.fetchFiles()
        }
    }

    fun uploadOfflineTrack(track: Track) {
        viewModelScope.launch {
            val filePath = track.offlineFilePath ?: return@launch
            val file = File(filePath)
            if (!file.exists()) {
                _uiState.update { it.copy(message = "File not found: ${track.title}") }
                return@launch
            }
            val fileName = "${track.title} - ${track.artist}.m4a"
                .replace(Regex("[^a-zA-Z0-9._\\- ]"), "")
            megaApiClient.uploadFile(file, fileName)
        }
    }

    fun downloadFromCloud(node: MegaNode) {
        viewModelScope.launch {
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "music")
            if (!musicDir.exists()) musicDir.mkdirs()

            val destFile = File(musicDir, node.name)
            val result = megaApiClient.downloadFile(node, destFile)
            result.onSuccess { file ->
                // Save as offline track in DB
                val videoId = "cloud_${node.handle}"
                val title = node.name.substringBeforeLast('.')
                val track = Track(
                    videoId = videoId,
                    title = title,
                    artist = "Cloud",
                    thumbnailUrl = "",
                    durationMs = 0L,
                    isOffline = true,
                    offlineFilePath = file.absolutePath
                )
                trackRepository.insertTrack(track)
                trackRepository.setOffline(videoId, true, file.absolutePath)
                _uiState.update { it.copy(message = "Downloaded: ${node.name}") }
            }.onFailure { e ->
                _uiState.update { it.copy(message = "Download failed: ${e.message}") }
            }
        }
    }

    fun deleteFromCloud(node: MegaNode) {
        viewModelScope.launch {
            megaApiClient.deleteNode(node.handle)
            _uiState.update { it.copy(deleteConfirmNode = null, message = "Deleted: ${node.name}") }
        }
    }

    fun confirmDelete(node: MegaNode) {
        _uiState.update { it.copy(deleteConfirmNode = node) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirmNode = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearLoginError() {
        _uiState.update { it.copy(loginError = null) }
    }
}
