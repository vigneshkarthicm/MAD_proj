package com.example.skilldiscovery.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WarmAccent,
    onPrimary = CharcoalBlack,
    primaryContainer = WarmAccentSoft,
    onPrimaryContainer = SoftWhite,
    secondary = SoftWhite,
    onSecondary = CharcoalBlack,
    tertiary = WarmAccent,
    onTertiary = CharcoalBlack,
    background = CharcoalBlack,
    onBackground = SoftWhite,
    surface = DeepSlate,
    onSurface = SoftWhite,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = Color(0xFFB9B3AA),
    outline = SoftBorder
)

private val LightColorScheme = lightColorScheme(
    primary = WarmAccent,
    onPrimary = LightSurface,
    primaryContainer = LightBorder,
    onPrimaryContainer = CharcoalBlack,
    secondary = CharcoalBlack,
    onSecondary = LightSurface,
    tertiary = WarmAccent,
    onTertiary = LightSurface,
    background = LightCharcoal,
    onBackground = CharcoalBlack,
    surface = LightSurface,
    onSurface = CharcoalBlack,
    surfaceVariant = LightBorder,
    onSurfaceVariant = Color(0xFF5E5A55),
    outline = LightBorder
)

@Composable
fun SkillDiscoveryTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}