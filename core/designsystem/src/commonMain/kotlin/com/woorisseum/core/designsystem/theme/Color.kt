package com.woorisseum.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 스카이블루 한 계열과 중립색만 사용한다. 오류·삭제는 Material의 의미 색을 유지한다.
// 메뉴·다이얼로그를 포함한 라이트 표면은 흰색으로 통일한다.
// 밝은 브랜드 면에는 진한 글자를, 텍스트·포커스에는 대비가 높은 같은 계열의 파랑을 사용한다.
internal val WoorisseumLightColors = lightColorScheme(
    primary = Color(0xFF285F8D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8CBCEB),
    onPrimaryContainer = Color(0xFF162F46),
    secondary = Color(0xFF285F8D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FD),
    onSecondaryContainer = Color(0xFF244E73),
    tertiary = Color(0xFF285F8D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8E9FA),
    onTertiaryContainer = Color(0xFF244E73),
    background = Color.White,
    onBackground = Color(0xFF252A30),
    surface = Color.White,
    onSurface = Color(0xFF252A30),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF606872),
    surfaceTint = Color(0xFF285F8D),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE0E3E7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFF747D88),
    outlineVariant = Color(0xFFE4E8ED),
    inverseSurface = Color(0xFF303740),
    inverseOnSurface = Color(0xFFF5F7FA),
    inversePrimary = Color(0xFF8CBCEB),
)

// 기존 다크 모드 지원은 유지하고, 녹색 기운을 제거한 차콜·블루로 대응한다.
internal val WoorisseumDarkColors = darkColorScheme(
    primary = Color(0xFF8CBCEB),
    onPrimary = Color(0xFF162F46),
    primaryContainer = Color(0xFF8CBCEB),
    onPrimaryContainer = Color(0xFF162F46),
    secondary = Color(0xFF8CBCEB),
    onSecondary = Color(0xFF162F46),
    secondaryContainer = Color(0xFF31485E),
    onSecondaryContainer = Color(0xFFD8E9FA),
    tertiary = Color(0xFF8CBCEB),
    onTertiary = Color(0xFF162F46),
    tertiaryContainer = Color(0xFF43627F),
    onTertiaryContainer = Color(0xFFE4F0FC),
    background = Color(0xFF282E35),
    onBackground = Color(0xFFF0F3F7),
    surface = Color(0xFF303840),
    onSurface = Color(0xFFF0F3F7),
    surfaceVariant = Color(0xFF3D4854),
    onSurfaceVariant = Color(0xFFC4CDD6),
    surfaceTint = Color(0xFF8CBCEB),
    surfaceBright = Color(0xFF4C5967),
    surfaceDim = Color(0xFF222930),
    surfaceContainerLowest = Color(0xFF222930),
    surfaceContainerLow = Color(0xFF303840),
    surfaceContainer = Color(0xFF36404A),
    surfaceContainerHigh = Color(0xFF3F4B57),
    surfaceContainerHighest = Color(0xFF485663),
    outline = Color(0xFFA0ADBA),
    outlineVariant = Color(0xFF546170),
    inverseSurface = Color(0xFFE4EAF0),
    inverseOnSurface = Color(0xFF29343F),
    inversePrimary = Color(0xFF285F8D),
)
