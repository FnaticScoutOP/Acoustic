package com.ytwrap.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ytwrap.app.ui.screens.MainScreen
import com.ytwrap.app.ui.screens.OnboardingScreen
import com.ytwrap.app.ui.theme.SpotifyBlack
import com.ytwrap.app.ui.theme.SpotifyGreen
import com.ytwrap.app.ui.theme.SpotifyGreenDark
import com.ytwrap.app.ui.theme.YTWrapTheme
import com.ytwrap.app.ui.viewmodel.LibraryViewModel
import com.ytwrap.app.ui.viewmodel.PlayerViewModel
import com.ytwrap.app.ui.viewmodel.StatsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.math.sin

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — playback works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val prefs = getSharedPreferences("acoustic_prefs", Context.MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_done", false)

        setContent {
            YTWrapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showOnboarding by remember { mutableStateOf(!onboardingDone) }
                    var showComposeSplash by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(2500)
                        showComposeSplash = false
                    }

                    when {
                        showComposeSplash -> AcousticSplashScreen()
                        showOnboarding -> OnboardingScreen(
                            onFinish = {
                                prefs.edit().putBoolean("onboarding_done", true).apply()
                                showOnboarding = false
                            }
                        )
                        else -> {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            val libraryViewModel: LibraryViewModel = hiltViewModel()
                            val statsViewModel: StatsViewModel = hiltViewModel()

                            MainScreen(
                                playerViewModel = playerViewModel,
                                libraryViewModel = libraryViewModel,
                                statsViewModel = statsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcousticSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(com.ytwrap.app.R.mipmap.ic_launcher)
                .build(),
            contentDescription = "Acoustic",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Sine wave animation
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp)
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            val amplitude = height * 0.35f
            val frequency = 3f

            // Draw 3 layered sine waves
            listOf(
                Pair(SpotifyGreen, 1f),
                Pair(SpotifyGreenDark, 0.6f),
                Pair(SpotifyGreen.copy(alpha = 0.3f), 0.3f)
            ).forEachIndexed { index, (color, alphaMultiplier) ->
                val path = Path()
                val phaseOffset = phase + index * 1.2f
                val amp = amplitude * alphaMultiplier
                path.moveTo(0f, midY + amp * sin(phaseOffset).toFloat())
                for (x in 1..width.toInt()) {
                    val xf = x.toFloat()
                    val angle = (xf / width) * frequency * 2f * Math.PI.toFloat() + phaseOffset
                    path.lineTo(xf, midY + amp * sin(angle.toDouble()).toFloat())
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
