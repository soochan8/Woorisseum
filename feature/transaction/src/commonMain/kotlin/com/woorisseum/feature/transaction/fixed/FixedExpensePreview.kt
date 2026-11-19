package com.woorisseum.feature.transaction.fixed

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.core.domain.transaction.fixed.*
import kotlinx.datetime.YearMonth

// 예시 계획·납부 기록은 Preview에만 둔다. 운영 앱의 초기 데이터로 사용하지 않는다.
private fun previewPlans() = listOf(
    FixedExpensePlan("preview-rent", FixedExpenseType.RENT, null, 750_000, 1, YearMonth(2026, 11)),
    FixedExpensePlan("preview-phone", FixedExpenseType.TELECOMMUNICATION, "휴대폰 요금", 59_000, 15, YearMonth(2026, 11)),
    FixedExpensePlan("preview-subscription", FixedExpenseType.SUBSCRIPTION, "넷플릭스", 17_000, 31, YearMonth(2026, 11)),
)
private fun previewList() = FixedExpenseListUiState(YearMonth(2027, 2), CalculateMonthlyFixedExpensesUseCase()(
    YearMonth(2027, 2), previewPlans(), setOf("preview-rent")).toUiModel())

@Preview(name = "고정지출 · 목록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun FixedListPreview() = WoorisseumTheme { FixedExpenseListScreen(previewList(), {}, {}) }

@Preview(name = "고정지출 · 조회 미연결", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun FixedUnavailablePreview() = WoorisseumTheme {
    FixedExpenseListScreen(FixedExpenseListUiState(YearMonth(2026, 11)), {}, {})
}

@Preview(name = "고정지출 · 빈 목록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun FixedEmptyPreview() = WoorisseumTheme {
    FixedExpenseListScreen(FixedExpenseListUiState(YearMonth(2026, 11),
        CalculateMonthlyFixedExpensesUseCase()(YearMonth(2026, 11), emptyList()).toUiModel()), {}, {})
}

@Preview(name = "고정지출 · 등록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun FixedRegisterPreview() = WoorisseumTheme {
    FixedExpenseEditorScreen(FixedExpenseEditorUiState(null, FixedExpenseFormUiState(YearMonth(2026, 11))), {})
}

@Preview(name = "고정지출 · 수정", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun FixedEditPreview() = WoorisseumTheme {
    val plan = previewPlans().last()
    FixedExpenseEditorScreen(FixedExpenseEditorUiState(plan.id, plan.toFormUiState()), {})
}

@Preview(name = "고정지출 · 직접 입력 오류", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun FixedErrorPreview() = WoorisseumTheme {
    FixedExpenseEditorScreen(FixedExpenseEditorUiState(null,
        FixedExpenseFormUiState(YearMonth(2026, 11), type = FixedExpenseType.OTHER, nameRequired = true,
            billingDayInput = "32", invalidBillingDay = true)), {})
}

@Preview(name = "고정지출 · 수정 미연결", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun FixedEditUnavailablePreview() = WoorisseumTheme {
    FixedExpenseEditorScreen(FixedExpenseEditorUiState("preview-plan", null), {})
}

@Preview(name = "고정지출 · 다크", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun FixedDarkPreview() = WoorisseumTheme(darkTheme = true) { FixedExpenseListScreen(previewList(), {}, {}) }
