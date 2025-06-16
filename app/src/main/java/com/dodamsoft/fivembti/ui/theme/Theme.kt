package com.dodamsoft.fivembti.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 부드러운 BlueGray/Teal 기반 팔레트
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006D77),        // Teal
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00332C),

    secondary = Color(0xFF83C5BE),      // Light mint
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE0FBFC),
    onSecondaryContainer = Color(0xFF003B3B),

    tertiary = Color(0xFFFFB703),       // Soft orange
    onTertiary = Color.Black,
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB2DFDB),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color.White,

    secondary = Color(0xFF006D77),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF264653),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFFFFB703),
    onTertiary = Color.Black,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun FiveMbtiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
