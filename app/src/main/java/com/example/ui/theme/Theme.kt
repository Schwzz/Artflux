package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.AppTheme
import com.example.model.ColorPalette

private val SolarAmberDarkColorScheme = darkColorScheme(
    primary = SolarAmberPrimaryDark,
    onPrimary = SolarAmberOnPrimaryDark,
    primaryContainer = SolarAmberPrimaryContainerDark,
    onPrimaryContainer = SolarAmberOnPrimaryContainerDark,
    secondary = SolarAmberSecondaryDark,
    onSecondary = SolarAmberOnSecondaryDark,
    secondaryContainer = SolarAmberSecondaryContainerDark,
    onSecondaryContainer = SolarAmberOnSecondaryContainerDark,
    tertiary = SolarAmberTertiaryDark,
    background = SolarAmberBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SolarAmberSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SolarAmberSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SolarAmberOutlineDark
)

private val SolarAmberLightColorScheme = lightColorScheme(
    primary = SolarAmberPrimaryLight,
    onPrimary = SolarAmberOnPrimaryLight,
    primaryContainer = SolarAmberPrimaryContainerLight,
    onPrimaryContainer = SolarAmberOnPrimaryContainerLight,
    secondary = SolarAmberSecondaryLight,
    onSecondary = SolarAmberOnSecondaryLight,
    secondaryContainer = SolarAmberSecondaryContainerLight,
    onSecondaryContainer = SolarAmberOnSecondaryContainerLight,
    tertiary = SolarAmberTertiaryLight,
    background = SolarAmberBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SolarAmberSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SolarAmberSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SolarAmberOutlineLight
)

private val AuroraEmeraldDarkColorScheme = darkColorScheme(
    primary = AuroraEmeraldPrimaryDark,
    onPrimary = AuroraEmeraldOnPrimaryDark,
    primaryContainer = AuroraEmeraldPrimaryContainerDark,
    onPrimaryContainer = AuroraEmeraldOnPrimaryContainerDark,
    secondary = AuroraEmeraldSecondaryDark,
    onSecondary = AuroraEmeraldOnSecondaryDark,
    secondaryContainer = AuroraEmeraldSecondaryContainerDark,
    onSecondaryContainer = AuroraEmeraldOnSecondaryContainerDark,
    tertiary = AuroraEmeraldTertiaryDark,
    background = AuroraEmeraldBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = AuroraEmeraldSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = AuroraEmeraldSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = AuroraEmeraldOutlineDark
)

private val AuroraEmeraldLightColorScheme = lightColorScheme(
    primary = AuroraEmeraldPrimaryLight,
    onPrimary = AuroraEmeraldOnPrimaryLight,
    primaryContainer = AuroraEmeraldPrimaryContainerLight,
    onPrimaryContainer = AuroraEmeraldOnPrimaryContainerLight,
    secondary = AuroraEmeraldSecondaryLight,
    onSecondary = AuroraEmeraldOnSecondaryLight,
    secondaryContainer = AuroraEmeraldSecondaryContainerLight,
    onSecondaryContainer = AuroraEmeraldOnSecondaryContainerLight,
    tertiary = AuroraEmeraldTertiaryLight,
    background = AuroraEmeraldBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = AuroraEmeraldSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = AuroraEmeraldSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = AuroraEmeraldOutlineLight
)

@Composable
fun MediaBrowserTheme(
    theme: AppTheme = AppTheme.DARK,
    colorPalette: ColorPalette = ColorPalette.SOLAR_AMBER,
    content: @Composable () -> Unit
) {
    val isLight = theme == AppTheme.LIGHT
    val colorScheme: ColorScheme = when (colorPalette) {
        ColorPalette.SOLAR_AMBER -> if (isLight) SolarAmberLightColorScheme else SolarAmberDarkColorScheme
        ColorPalette.AURORA_EMERALD -> if (isLight) AuroraEmeraldLightColorScheme else AuroraEmeraldDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
