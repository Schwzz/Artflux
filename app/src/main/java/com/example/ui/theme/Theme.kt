package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkAmoledColorScheme = darkColorScheme(
    primary = NeonIndigo,
    onPrimary = TextPrimary,
    primaryContainer = NeonIndigoDark,
    onPrimaryContainer = NeonIndigoLight,
    secondary = CyanAccent,
    onSecondary = DarkBackground,
    secondaryContainer = CyanAccentDark,
    onSecondaryContainer = CyanAccentLight,
    tertiary = MagentaAccent,
    background = Color(0xFF000000),
    onBackground = TextPrimary,
    surface = Color(0xFF0B0F17),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF131A29),
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

private val DarkObsidianColorScheme = darkColorScheme(
    primary = NeonIndigo,
    onPrimary = TextPrimary,
    primaryContainer = NeonIndigoDark,
    onPrimaryContainer = NeonIndigoLight,
    secondary = CyanAccent,
    onSecondary = DarkBackground,
    secondaryContainer = CyanAccentDark,
    onSecondaryContainer = CyanAccentLight,
    tertiary = MagentaAccent,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun MediaBrowserTheme(
    darkAmoled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkAmoled) DarkAmoledColorScheme else DarkObsidianColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
