package com.medit.health.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MedItDarkGreen,
    onPrimary = SurfacePure,
    primaryContainer = MedItMintLight,
    onPrimaryContainer = MedItDarkGreen,
    secondary = MedItPrimary700,
    onSecondary = SurfacePure,
    secondaryContainer = MedItMintSoft,
    onSecondaryContainer = MedItPrimary800,
    tertiary = MedItEmerald,
    onTertiary = SurfacePure,
    background = BackgroundWarm,
    onBackground = TextPrimary,
    surface = SurfacePure,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderLight,
    error = StatusErrorRed,
    onError = SurfacePure
)

private val DarkColorScheme = darkColorScheme(
    primary = MedItMintAccent,
    onPrimary = MedItDarkGreen,
    primaryContainer = MedItPrimary700,
    onPrimaryContainer = MedItMintLight,
    secondary = MedItEmerald,
    onSecondary = MedItDarkGreen,
    background = Color(0xFF0F1713),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF16231C),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1E3127),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF2C4437),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

@Composable
fun MedItTheme(
    darkTheme: Boolean = false, // Always prefer professional clinical bright theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = MedItDarkGreen.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
