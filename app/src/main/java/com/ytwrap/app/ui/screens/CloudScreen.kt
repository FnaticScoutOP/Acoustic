package com.ytwrap.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ytwrap.app.cloud.MegaNode
import com.ytwrap.app.data.entity.Track
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyGray
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyLightGray
import com.ytwrap.app.ui.theme.SpotifyTextSecondary
import com.ytwrap.app.ui.viewmodel.CloudViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudScreen(
    cloudViewModel: CloudViewModel,
    onBack: () -> Unit
) {
    val isLoggedIn by cloudViewModel.isLoggedIn.collectAsState()
    val loggedInEmail by cloudViewModel.loggedInEmail.collectAsState()
    val cloudFiles by cloudViewModel.cloudFiles.collectAsState()
    val isLoading by cloudViewModel.isLoading.collectAsState()
    val uiState by cloudViewModel.uiState.collectAsState()
    val uploadProgress by cloudViewModel.uploadProgress.collectAsState()
    val downloadProgress by cloudViewModel.downloadProgress.collectAsState()
    val offlineTracks by cloudViewModel.offlineTracks.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showUploadPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            cloudViewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = SpotifyBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Cloud Storage",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (isLoggedIn) {
                            Text(
                                loggedInEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = SpotifyTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = { cloudViewModel.refreshFiles() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                        IconButton(onClick = { cloudViewModel.logout() }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Upload/Download progress bars
            uploadProgress?.let { progress ->
                ProgressBar(
                    label = "Uploading: ${progress.fileName}",
                    progress = progress.progress,
                    error = progress.error,
                    isUpload = true
                )
            }
            downloadProgress?.let { progress ->
                ProgressBar(
                    label = "Downloading: ${progress.fileName}",
                    progress = progress.progress,
                    error = progress.error,
                    isUpload = false
                )
            }

            if (!isLoggedIn) {
                MegaLoginForm(
                    isLoggingIn = uiState.isLoggingIn,
                    loginError = uiState.loginError,
                    onLogin = { email, password -> cloudViewModel.login(email, password) },
                    onClearError = { cloudViewModel.clearLoginError() }
                )
            } else {
                // Upload section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showUploadPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload to Cloud", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }

                HorizontalDivider(color = SpotifyGray, thickness = 1.dp)

                // Cloud files list
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SpotifyGreen)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading cloud files...", color = SpotifyTextSecondary)
                        }
                    }
                } else if (cloudFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Cloud,
                                contentDescription = null,
                                tint = SpotifyGreen.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No audio files in cloud",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Upload your downloaded songs to MEGA",
                                color = SpotifyTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Text(
                        "Cloud Files (${cloudFiles.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    LazyColumn {
                        items(cloudFiles, key = { it.handle }) { node ->
                            CloudFileItem(
                                node = node,
                                onDownload = { cloudViewModel.downloadFromCloud(node) },
                                onDelete = { cloudViewModel.confirmDelete(node) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Upload picker dialog - shows downloaded tracks to upload
    if (showUploadPicker) {
        UploadPickerDialog(
            offlineTracks = offlineTracks,
            onUpload = { track ->
                cloudViewModel.uploadOfflineTrack(track)
                showUploadPicker = false
            },
            onDismiss = { showUploadPicker = false }
        )
    }

    // Delete confirmation dialog
    if (uiState.deleteConfirmNode != null) {
        val node = uiState.deleteConfirmNode!!
        AlertDialog(
            onDismissRequest = { cloudViewModel.cancelDelete() },
            title = { Text("Delete from cloud?", color = Color.White) },
            text = {
                Text(
                    "\"${node.name}\" will be permanently deleted from your MEGA cloud storage.",
                    color = SpotifyTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { cloudViewModel.deleteFromCloud(node) }) {
                    Text("Delete", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { cloudViewModel.cancelDelete() }) {
                    Text("Cancel", color = SpotifyGreen)
                }
            },
            containerColor = SpotifyGray,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ProgressBar(
    label: String,
    progress: Float,
    error: String?,
    isUpload: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpotifyGray)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isUpload) Icons.Filled.CloudUpload else Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = if (error != null) Color(0xFFE57373) else SpotifyGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error ?: label,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) Color(0xFFE57373) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (error == null) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyGreen
                )
            }
        }
        if (error == null) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = SpotifyGreen,
                trackColor = SpotifyLightGray,
            )
        }
    }
}

@Composable
private fun MegaLoginForm(
    isLoggingIn: Boolean,
    loginError: String?,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // MEGA logo placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9272E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "M",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Sign in to MEGA",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                "Connect your MEGA cloud storage",
                style = MaterialTheme.typography.bodyMedium,
                color = SpotifyTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = SpotifyLightGray,
                cursorColor = SpotifyGreen,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = SpotifyGreen,
                unfocusedLabelColor = SpotifyTextSecondary,
                focusedLeadingIconColor = SpotifyGreen,
                unfocusedLeadingIconColor = SpotifyTextSecondary,
                focusedTrailingIconColor = SpotifyGreen,
                unfocusedTrailingIconColor = SpotifyTextSecondary
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; onClearError() },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; onClearError() },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide" else "Show"
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (email.isNotBlank() && password.isNotBlank()) onLogin(email, password)
                }),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (loginError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = loginError,
                    color = Color(0xFFE57373),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLogin(email, password) },
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoggingIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    disabledContainerColor = SpotifyGreen.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isLoggingIn) "Signing in..." else "Sign in",
                    color = if (isLoggingIn) Color.Black.copy(alpha = 0.7f) else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Don't have a MEGA account? Sign up at mega.nz",
                style = MaterialTheme.typography.labelSmall,
                color = SpotifyTextSecondary
            )
        }
    }
}

@Composable
private fun CloudFileItem(
    node: MegaNode,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SpotifyGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cloud, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "MEGA • ${formatCloudFileSize(node.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextSecondary
                )
            }
        }

        IconButton(onClick = onDownload) {
            Icon(
                Icons.Filled.CloudDownload,
                contentDescription = "Download from cloud",
                tint = SpotifyGreen,
                modifier = Modifier.size(22.dp)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete from cloud",
                tint = SpotifyTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun UploadPickerDialog(
    offlineTracks: List<Track>,
    onUpload: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Upload to MEGA", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            if (offlineTracks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.CloudUpload,
                        contentDescription = null,
                        tint = SpotifyTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No downloaded songs to upload",
                        color = SpotifyTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Download songs first, then upload them to the cloud",
                        color = SpotifyTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                LazyColumn {
                    items(offlineTracks, key = { it.videoId }) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpload(track) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpotifyTextSecondary,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                Icons.Filled.CloudUpload,
                                contentDescription = "Upload",
                                tint = SpotifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SpotifyGreen)
            }
        },
        containerColor = SpotifyGray,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun formatCloudFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
