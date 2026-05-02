package com.ytwrap.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Spotify-inspired palette ────────────────────────────────────────────
val SpotifyGreen       = Color(0xFF1DB954)
val SpotifyGreenLight  = Color(0xFF1ED760)
val SpotifyGreenDark   = Color(0xFF158C3E)
val SpotifyBlack       = Color(0xFF121212)
val SpotifyDarkGray    = Color(0xFF181818)
val SpotifyGray        = Color(0xFF282828)
val SpotifyLightGray   = Color(0xFF404040)
val SpotifyTextPrimary = Color(0xFFFFFFFF)
val SpotifyTextSecondary = Color(0xFFB3B3B3)
val SpotifyLikeColor   = Color(0xFF1DB954)

private val SpotifyColorScheme = darkColorScheme(
    primary            = SpotifyGreen,
    onPrimary          = Color.Black,
    primaryContainer   = SpotifyGreenDark,
    onPrimaryContainer = SpotifyGreenLight,
    secondary          = SpotifyGreenLight,
    onSecondary        = Color.Black,
    secondaryContainer = SpotifyGray,
    onSecondaryContainer = SpotifyTextPrimary,
    tertiary           = Color(0xFF1ED760),
    background         = SpotifyBlack,
    onBackground       = SpotifyTextPrimary,
    surface            = SpotifyDarkGray,
    onSurface          = SpotifyTextPrimary,
    surfaceVariant     = SpotifyGray,
    onSurfaceVariant   = SpotifyTextSecondary,
    surfaceTint        = SpotifyGreen,
    inverseSurface     = SpotifyTextPrimary,
    inverseOnSurface   = SpotifyBlack,
    error              = Color(0xFFE57373),
    onError            = Color.Black,
    outline            = SpotifyLightGray,
    outlineVariant     = Color(0xFF333333)
)

private val SpotifyTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = SpotifyTextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.25).sp,
        color = SpotifyTextPrimary
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = SpotifyTextPrimary
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.25).sp,
        color = SpotifyTextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = SpotifyTextPrimary
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = SpotifyTextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = SpotifyTextPrimary
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = SpotifyTextSecondary
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        color = SpotifyTextPrimary
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = SpotifyTextSecondary
    )
)

// ── Gradient brushes for dispersion / glow effects ──────────────────────
val SpotifyGradientVertical = Brush.verticalGradient(
    colors = listOf(
        SpotifyGreen.copy(alpha = 0.6f),
        SpotifyGreenDark.copy(alpha = 0.3f),
        SpotifyBlack
    )
)

val SpotifyGradientRadial = Brush.radialGradient(
    colors = listOf(
        SpotifyGreen.copy(alpha = 0.35f),
        SpotifyBlack.copy(alpha = 0f)
    )
)

val MiniPlayerGradient = Brush.horizontalGradient(
    colors = listOf(
        SpotifyGray,
        SpotifyGreenDark.copy(alpha = 0.25f),
        SpotifyGray
    )
)

@Composable
fun YTWrapTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SpotifyBlack.toArgb()
            window.navigationBarColor = SpotifyBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SpotifyColorScheme,
        typography = SpotifyTypography,
        content = content
    )
}

val GradientHueColors = listOf(
    Color(0xFF1DB954), // green
    Color(0xFF8B5CF6), // purple
    Color(0xFFE67E22), // warm orange
    Color(0xFF14B8A6), // teal
    Color(0xFFE84393), // rose
    Color(0xFF3B82F6), // blue
)

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var colorIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            colorIndex = (colorIndex + 1) % GradientHueColors.size
        }
    }
    val animatedColor by animateColorAsState(
        targetValue = GradientHueColors[colorIndex].copy(alpha = 0.30f),
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "gradientHue"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedColor, SpotifyBlack),
                    startY = 0f,
                    endY = 600f
                )
            ),
        content = content
    )
}
