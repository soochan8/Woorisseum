package com.woorisseum.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 화면은 MaterialTheme.typography를 사용한다. 색상과 금액·날짜 표시 형식은 포함하지 않는다. */
internal val WoorisseumTypography = Typography(
    // 월 합계 등 핵심 금액. tnum은 지원 글꼴에서 숫자 폭을 일정하게 한다.
    displaySmall = appTextStyle(32, 40, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"),
    headlineMedium = appTextStyle(24, 32, FontWeight.SemiBold),
    headlineSmall = appTextStyle(22, 30, FontWeight.SemiBold), // 화면 제목
    titleLarge = appTextStyle(20, 28, FontWeight.SemiBold), // 시트·대화상자 제목
    titleMedium = appTextStyle(16, 24, FontWeight.SemiBold), // 섹션 제목
    titleSmall = appTextStyle(15, 22, FontWeight.Medium),
    bodyLarge = appTextStyle(16, 24, FontWeight.Normal), // 목록·입력·본문
    bodyMedium = appTextStyle(14, 22, FontWeight.Normal), // 설명 문장
    bodySmall = appTextStyle(13, 20, FontWeight.Normal), // 날짜·결제수단·보조 정보
    labelLarge = appTextStyle(15, 22, FontWeight.Medium), // 버튼·필터
    labelMedium = appTextStyle(13, 18, FontWeight.Medium),
    labelSmall = appTextStyle(12, 16, FontWeight.Medium), // 짧은 분류 표시
)

/** 핵심 금액에 붙는 통화 단위. 금액보다 작고 가볍게 표시한다. */
val Typography.amountUnit: TextStyle
    get() = headlineSmall.copy(fontWeight = FontWeight.Normal)

/** 목록의 금액 열. 본문 크기를 유지하며 굵기와 숫자 폭으로 구분한다. */
val Typography.listAmount: TextStyle
    get() = bodyLarge.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")

/** sp를 사용해 사용자 글자 크기를 존중한다. 한글 자간을 임의로 압축하지 않는다. */
private fun appTextStyle(size: Int, lineHeight: Int, weight: FontWeight): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = 0.sp,
)
