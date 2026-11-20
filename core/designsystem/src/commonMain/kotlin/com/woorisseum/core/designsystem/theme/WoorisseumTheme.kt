package com.woorisseum.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun WoorisseumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) WoorisseumDarkColors else WoorisseumLightColors,
        typography = WoorisseumTypography,
        shapes = WoorisseumShapes,
        content = content,
    )
}
