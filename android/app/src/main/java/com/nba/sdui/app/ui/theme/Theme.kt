package com.nba.sdui.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// NBA Brand Colors
private val NbaBlue = Color(0xFF1D428A)
private val NbaRed = Color(0xFFC8102E)
private val NbaWhite = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = NbaBlue,
    secondary = NbaRed,
    tertiary = Color(0xFF7B8D93),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = NbaWhite,
    onSecondary = NbaWhite,
    onTertiary = NbaWhite,
    onBackground = NbaWhite,
    onSurface = NbaWhite
)

private val LightColorScheme = lightColorScheme(
    primary = NbaBlue,
    secondary = NbaRed,
    tertiary = Color(0xFF5A6B70),
    background = Color(0xFFF5F5F5),
    surface = NbaWhite,
    onPrimary = NbaWhite,
    onSecondary = NbaWhite,
    onTertiary = NbaWhite,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun SduiPrototypeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        content = content
    )
}
