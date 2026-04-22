package com.findmygym.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = Blue80,
    tertiary = Grey80,

    surface = DarkSurface,
    background = DarkBackground,
    onSurface = White,
    onBackground = White,

    secondaryContainer = Green40,
    onSecondaryContainer = White,
    surfaceVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = Blue40,
    tertiary = Grey40,

    surface = LightSurface,
    background = LightBackground,
    onSurface = Black,
    onBackground = Black,

    secondaryContainer = Green80,
    onSecondaryContainer = Black,
    surfaceVariant = LightSurfaceVariant
)

@Composable
fun FindMyGymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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