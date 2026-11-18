package com.woorisseum.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3C654D),
    onPrimary = Color(0xFFFFFEFA),
    primaryContainer = Color(0xFFDDEADD),
    onPrimaryContainer = Color(0xFF243D2E),
    secondaryContainer = Color(0xFFE1E9DE),
    onSecondaryContainer = Color(0xFF293E2F),
    surfaceContainerHigh = Color(0xFFECEFE7),
    background = Color(0xFFFFFEFA),
    onBackground = Color(0xFF222A26),
    surface = Color(0xFFFFFEFA),
    onSurface = Color(0xFF222A26),
    surfaceVariant = Color(0xFFF3F4EE),
    onSurfaceVariant = Color(0xFF737B74),
    outline = Color(0xFF858E85),
    outlineVariant = Color(0xFFEAECE6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBFD4A9),
    onPrimary = Color(0xFF202D20),
    primaryContainer = Color(0xFF304A37),
    onPrimaryContainer = Color(0xFFDDEADD),
    secondaryContainer = Color(0xFF344537),
    onSecondaryContainer = Color(0xFFE1E9DE),
    surfaceContainerHigh = Color(0xFF303831),
    background = Color(0xFF1D211F),
    onBackground = Color(0xFFECF0EA),
    surface = Color(0xFF1D211F),
    onSurface = Color(0xFFECF0EA),
    surfaceVariant = Color(0xFF282F29),
    onSurfaceVariant = Color(0xFFA1AAA2),
    outline = Color(0xFF7B867D),
    outlineVariant = Color(0xFF343C35),
)

@Composable
fun WoorisseumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
