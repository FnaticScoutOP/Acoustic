package com.ytwrap.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
