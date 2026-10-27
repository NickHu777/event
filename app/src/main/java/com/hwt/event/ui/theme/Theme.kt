package com.hwt.event.ui.theme

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

enum class EventPalette(val label: String, val preferenceValue: String, val previewColor: Color) {
    GREEN("绿色", "green", EventGreen),
    BLUE("蓝色", "blue", Color(0xFF315F9E));

    companion object {
        fun fromPreference(value: String?): EventPalette =
            entries.firstOrNull { it.preferenceValue == value } ?: GREEN
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BCDB8),
    onPrimary = Color(0xFF0C382F),
    primaryContainer = Color(0xFF174D41),
    onPrimaryContainer = Color(0xFFD3F0E3),
    secondary = Color(0xFFB4CFC2),
    onSecondary = Color(0xFF243A31),
    secondaryContainer = Color(0xFF25443A),
    onSecondaryContainer = Color(0xFFD3F0E3),
    tertiary = Color(0xFFE3B780),
    tertiaryContainer = Color(0xFF543D24),
    onTertiaryContainer = Color(0xFFF5DFBF),
    background = Color(0xFF101915),
    onBackground = Color(0xFFE2E9E3),
    surface = Color(0xFF151F1A),
    onSurface = Color(0xFFE2E9E3),
    surfaceVariant = Color(0xFF26342D),
    onSurfaceVariant = Color(0xFFBECCC3),
    outline = Color(0xFF87968D),
    outlineVariant = Color(0xFF43534A),
    surfaceTint = Color(0xFF9BCDB8),
    inversePrimary = Color(0xFF245B4F)
)

private val GreenLightColorScheme = lightColorScheme(
    primary = EventGreen,
    onPrimary = Color.White,
    primaryContainer = EventMint,
    onPrimaryContainer = EventGreenDeep,
    secondary = Color(0xFF55756A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EEE7),
    onSecondaryContainer = EventGreenDeep,
    tertiary = EventAmber,
    tertiaryContainer = Color(0xFFF6E8D9),
    onTertiaryContainer = Color(0xFF5B3517),
    background = EventCanvas,
    onBackground = EventInk,
    surface = Color(0xFFFCFDFB),
    onSurface = EventInk,
    surfaceVariant = Color(0xFFE7EEE9),
    onSurfaceVariant = EventMuted,
    outline = Color(0xFF7B8B82),
    outlineVariant = Color(0xFFD5E1D9),
    surfaceTint = EventGreen,
    inversePrimary = Color(0xFFA9D9C2)
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF102F55),
    primaryContainer = Color(0xFF24486F),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFFB9C9E1),
    onSecondary = Color(0xFF26384E),
    secondaryContainer = Color(0xFF354A62),
    onSecondaryContainer = Color(0xFFDCE6F4),
    tertiary = Color(0xFFE3B780),
    tertiaryContainer = Color(0xFF543D24),
    onTertiaryContainer = Color(0xFFF5DFBF),
    background = Color(0xFF101820),
    onBackground = Color(0xFFE2E9F2),
    surface = Color(0xFF151F29),
    onSurface = Color(0xFFE2E9F2),
    surfaceVariant = Color(0xFF293744),
    onSurfaceVariant = Color(0xFFBECBD9),
    outline = Color(0xFF8796A6),
    outlineVariant = Color(0xFF435363),
    surfaceTint = Color(0xFFA8C7FA),
    inversePrimary = Color(0xFF315F9E)
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF315F9E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FA),
    onPrimaryContainer = Color(0xFF183D6B),
    secondary = Color(0xFF526B89),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEE7F2),
    onSecondaryContainer = Color(0xFF243952),
    tertiary = EventAmber,
    tertiaryContainer = Color(0xFFF6E8D9),
    onTertiaryContainer = Color(0xFF5B3517),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF172331),
    surface = Color(0xFFFCFDFE),
    onSurface = Color(0xFF172331),
    surfaceVariant = Color(0xFFE8EDF4),
    onSurfaceVariant = Color(0xFF657587),
    outline = Color(0xFF7C8999),
    outlineVariant = Color(0xFFD6DEE8),
    surfaceTint = Color(0xFF315F9E),
    inversePrimary = Color(0xFFB6CFF7)
)

@Composable
fun EventTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    palette: EventPalette = EventPalette.GREEN,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        palette == EventPalette.BLUE && darkTheme -> BlueDarkColorScheme
        palette == EventPalette.BLUE -> BlueLightColorScheme
        darkTheme -> DarkColorScheme
        else -> GreenLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
