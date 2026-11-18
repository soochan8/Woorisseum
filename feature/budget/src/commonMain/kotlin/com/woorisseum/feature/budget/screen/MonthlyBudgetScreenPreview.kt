package com.woorisseum.feature.budget.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.core.domain.budget.usecase.CalculateBudgetSummaryUseCase
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.feature.budget.mapper.toUiModel
import com.woorisseum.feature.budget.model.MonthlyBudgetUiState
import kotlinx.datetime.YearMonth

@Preview(name = "예산 · 기본", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun BudgetPreview() = BudgetPreviewContent(previewState(2_500_000))

@Preview(name = "예산 · 미설정", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun BudgetUnsetPreview() = BudgetPreviewContent(previewState(null))

@Preview(name = "예산 · 집계 대기", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun BudgetWaitingPreview() = BudgetPreviewContent(
    MonthlyBudgetUiState(YearMonth(2026, 12), CalculateBudgetSummaryUseCase()(2_500_000, null).toUiModel()),
)

@Preview(name = "예산 · 0원", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun BudgetZeroPreview() = BudgetPreviewContent(previewState(0, hasExpenses = false))

@Preview(name = "예산 · 지출 없음", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun BudgetNoExpensesPreview() = BudgetPreviewContent(previewState(2_500_000, hasExpenses = false))

@Preview(name = "예산 · 초과", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun BudgetExceededPreview() = BudgetPreviewContent(previewState(1_500_000))

@Preview(name = "예산 · 다크", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun BudgetDarkPreview() = BudgetPreviewContent(previewState(2_500_000), darkTheme = true)

@Composable
private fun BudgetPreviewContent(state: MonthlyBudgetUiState, darkTheme: Boolean = false) {
    WoorisseumTheme(darkTheme = darkTheme) {
        MonthlyBudgetScreen(state = state, onIntent = {}, onBack = {})
    }
}

// 확인용 금액은 Preview에만 둔다. 실제 앱의 초기 데이터나 저장 내역으로 사용하지 않는다.
private fun previewState(amount: Long?, hasExpenses: Boolean = true): MonthlyBudgetUiState {
    val categoryTotals = if (hasExpenses) mapOf(
        ExpenseCategory.FIXED to 1_050_000L,
        ExpenseCategory.VARIABLE to 670_000L,
        ExpenseCategory.IMPULSE to 80_000L,
    ) else emptyMap()
    return MonthlyBudgetUiState(
        month = YearMonth(2026, 12),
        summary = CalculateBudgetSummaryUseCase()(amount, categoryTotals).toUiModel(),
    )
}
