package com.woorisseum.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun WoorisseumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) WoorisseumDarkColors else WoorisseumLightColors,
        typography = WoorisseumTypography,
        shapes = WoorisseumShapes,
        content = {
            // 라이트 모드의 흰 메뉴·다이얼로그에 자동 색조가 덧씌워지지 않게 한다.
            CompositionLocalProvider(LocalTonalElevationEnabled provides darkTheme, content = content)
        },
    )
}
