package com.typesync.client.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// -- Custom palette --

private val Teal80 = Color(0xFF80CBC4)
private val Teal40 = Color(0xFF00897B)
private val Teal30 = Color(0xFF00695C)
private val Teal90 = Color(0xFFB2DFDB)

private val Blue80 = Color(0xFF90CAF9)
private val Blue40 = Color(0xFF1E88E5)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF1B3A36),
    onPrimaryContainer = Teal90,
    secondary = Blue80,
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFCCE5FF),
    tertiary = Color(0xFFFFB74D),
    tertiaryContainer = Color(0xFF3E2723),
    onTertiaryContainer = Color(0xFFFFE0B2),
    error = Color(0xFFEF9A9A),
    errorContainer = Color(0xFF5D1A1A),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF616161),
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Teal30,
    secondary = Blue40,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFFF57C00),
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFFE65100),
    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFCDD2),
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFECEFF1),
    onSurface = Color(0xFF212121),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFFBDBDBD),
)

enum class ThemeMode { LIGHT, DARK, AUTO }

@Composable
fun TypeSyncTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        // Use dynamic color on Android 12+
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
