package com.example.vesbp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF002FA7), onPrimary = Color.White,
    background = Color(0xFFF7F8FA), onBackground = Color(0xFF171B22),
    surface = Color.White, onSurface = Color(0xFF171B22),
    surfaceVariant = Color(0xFFF0F2F5), outline = Color(0xFFD8DCE3)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9DBBFF), onPrimary = Color(0xFF002A86),
    background = Color(0xFF111318), onBackground = Color(0xFFE4E8EF),
    surface = Color(0xFF1B1E25), onSurface = Color(0xFFE4E8EF),
    surfaceVariant = Color(0xFF252932), outline = Color(0xFF454B57)
)

@Composable
fun VeSBPTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme, typography = Typography, content = content)
}
