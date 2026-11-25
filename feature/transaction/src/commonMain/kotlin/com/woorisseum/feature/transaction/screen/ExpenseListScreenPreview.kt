package com.woorisseum.feature.transaction.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.feature.transaction.viewmodel.withExpenseResults
import com.woorisseum.feature.transaction.model.ExpenseSearchPeriod
import com.woorisseum.feature.transaction.model.ExpenseSearchPreset
import com.woorisseum.feature.transaction.model.ExpenseDateRange
import com.woorisseum.feature.transaction.model.ExpenseDayUiModel
import com.woorisseum.feature.transaction.model.ExpenseItemUiModel
import com.woorisseum.feature.transaction.model.ExpenseListUiState
import com.woorisseum.feature.transaction.model.ExpenseListFilters
import com.woorisseum.feature.transaction.model.ExpensePaymentFilter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

@Preview(name = "지출 목록 · 일상형", widthDp = 390, heightDp = 800, locale = "ko")
@Composable
private fun ExpenseListContentPreview() {
    WoorisseumTheme(darkTheme = false) {
        ExpenseListScreen(state = previewExpenseListState(), onIntent = {})
    }
}

@Preview(name = "지출 목록 · 빈 상태", widthDp = 390, heightDp = 800, locale = "ko")
@Composable
private fun ExpenseListEmptyPreview() {
    WoorisseumTheme(darkTheme = false) {
        ExpenseListScreen(state = ExpenseListUiState(month = YearMonth(2026, 11)), onIntent = {})
    }
}

@Preview(name = "지출 목록 · 다크", widthDp = 390, heightDp = 800, locale = "ko")
@Composable
private fun ExpenseListDarkPreview() {
    WoorisseumTheme(darkTheme = true) {
        ExpenseListScreen(state = previewExpenseListState(), onIntent = {})
    }
}

@Preview(name = "지출 목록 · 필터 결과 없음", widthDp = 390, heightDp = 800, locale = "ko")
@Composable
private fun ExpenseListFilteredEmptyPreview() {
    WoorisseumTheme(darkTheme = false) {
        ExpenseListScreen(
            state = previewExpenseListState().copy(
                expenseCount = 0,
                dayGroups = emptyList(),
                filters = ExpenseListFilters(paymentMethod = ExpensePaymentFilter.Cash),
            ),
            onIntent = {},
        )
    }
}

// 시안 확인 전용 데이터다. 앱 실행이나 실제 저장소의 초기 데이터로 사용하지 않는다.
private fun previewExpenseListState() = ExpenseListUiState(
    month = YearMonth(2026, 11),
    totalAmountText = "981,700",
    expenseCount = 8,
    hasExpensesInMonth = true,
    periodText = "11월 1일 — 13일",
    dayGroups = listOf(
        ExpenseDayUiModel(
            date = LocalDate(2026, 11, 13),
            dateLabel = "13일 금요일",
            totalAmountText = "37,300",
            expenses = listOf(
                ExpenseItemUiModel("preview-1", "동네 정육점", "32,800", ExpenseCategory.VARIABLE, "현대카드"),
                ExpenseItemUiModel("preview-2", "이디야커피", "4,500", ExpenseCategory.VARIABLE, "현금"),
            ),
        ),
        ExpenseDayUiModel(
            date = LocalDate(2026, 11, 12),
            dateLabel = "12일 목요일",
            totalAmountText = "107,400",
            expenses = listOf(
                ExpenseItemUiModel("preview-3", "이마트", "68,400", ExpenseCategory.VARIABLE, "신한카드"),
                ExpenseItemUiModel("preview-4", "무신사", "39,000", ExpenseCategory.IMPULSE, "현대카드"),
            ),
        ),
        ExpenseDayUiModel(
            date = LocalDate(2026, 11, 11),
            dateLabel = "11일 수요일",
            totalAmountText = "75,000",
            expenses = listOf(
                ExpenseItemUiModel("preview-5", "휴대폰 요금", "59,000", ExpenseCategory.FIXED, "계좌이체"),
                ExpenseItemUiModel("preview-6", "김밥천국", "16,000", ExpenseCategory.VARIABLE, "신한카드"),
            ),
        ),
        ExpenseDayUiModel(
            date = LocalDate(2026, 11, 10),
            dateLabel = "10일 화요일",
            totalAmountText = "762,000",
            expenses = listOf(
                ExpenseItemUiModel("preview-7", "월세", "750,000", ExpenseCategory.FIXED, "계좌이체"),
                ExpenseItemUiModel("preview-8", "동네 세탁소", "12,000", ExpenseCategory.VARIABLE, "현금"),
            ),
        ),
    ),
)

private fun previewExpenseSearchState(query: String = "커피", period: ExpenseSearchPeriod = ExpenseSearchPeriod()): ExpenseListUiState {
    val expenses = listOf(
        com.woorisseum.core.domain.transaction.model.Expense("search-1", 4500, LocalDate(2026,11,13), "이디야커피",
            ExpenseCategory.VARIABLE, com.woorisseum.core.domain.transaction.model.PaymentMethod.Cash, "출근길 커피"),
        com.woorisseum.core.domain.transaction.model.Expense("search-2", 13800, LocalDate(2026,11,12), "동네 빵집",
            ExpenseCategory.VARIABLE, com.woorisseum.core.domain.transaction.model.PaymentMethod.BankTransfer, "커피와 함께 먹을 빵"),
        com.woorisseum.core.domain.transaction.model.Expense("search-previous-month", 5200, LocalDate(2026,10,9), "동네 카페",
            ExpenseCategory.VARIABLE, com.woorisseum.core.domain.transaction.model.PaymentMethod.Cash, "지난달 커피"),
        com.woorisseum.core.domain.transaction.model.Expense("search-3", 2200, LocalDate(2026,11,12), "편의점",
            ExpenseCategory.IMPULSE, com.woorisseum.core.domain.transaction.model.PaymentMethod.Cash, "생수"),
    )
    return ExpenseListUiState(month = YearMonth(2026,11), isSearchOpen = true, searchQuery = query, searchPeriod = period)
        .withExpenseResults(expenses)
}

@Preview(name = "지출 검색 · 결과", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseSearchResultsPreview() = WoorisseumTheme {
    ExpenseListScreen(previewExpenseSearchState(), {})
}

@Preview(name = "지출 검색 · 결과 없음", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseSearchEmptyPreview() = WoorisseumTheme {
    ExpenseListScreen(previewExpenseSearchState("보험"), {})
}

@Preview(name = "지출 검색 · 조회 미연결", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseSearchUnavailablePreview() = WoorisseumTheme {
    ExpenseListScreen(ExpenseListUiState(YearMonth(2026,11), isSearchOpen = true, searchQuery = "커피"), {})
}

@Preview(name = "지출 검색 · 좁은 화면", widthDp = 320, heightDp = 640, locale = "ko")
@Composable
private fun ExpenseSearchNarrowPreview() = WoorisseumTheme {
    ExpenseListScreen(previewExpenseSearchState(), {})
}

@Preview(name = "지출 검색 · 다크", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseSearchDarkPreview() = WoorisseumTheme(darkTheme = true) {
    ExpenseListScreen(previewExpenseSearchState(), {})
}


@Preview(name = "지출 검색 · 여러 달 직접 선택", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseSearchCustomPeriodPreview() = WoorisseumTheme {
    ExpenseListScreen(previewExpenseSearchState(period = ExpenseSearchPeriod(ExpenseSearchPreset.CUSTOM,
        ExpenseDateRange(LocalDate(2026,10,1), LocalDate(2026,11,30)))), {})
}


@Preview(name = "지출 검색 · 기간 선택", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseSearchPeriodMenuPreview() = WoorisseumTheme {
    ExpenseSearchPeriodSheet(ExpenseSearchPeriod(), onSelect = {}, onDismiss = {})
}

/** 승인된 색상·이모지 비교 시안이다. 예시 금액·아바타는 운영 상태에 주입하지 않는다. */
private fun previewComparison() = com.woorisseum.feature.transaction.model.ExpenseComparisonUiModel(
    first = com.woorisseum.feature.transaction.model.ExpenseComparisonPersonUiModel("나", "🐱", "48,000", 0.96f),
    second = com.woorisseum.feature.transaction.model.ExpenseComparisonPersonUiModel("짝꿍", "🐶", "36,500", 0.73f),
    maximumAmountText = "50,000", midpointAmountText = "25,000",
)

private fun skyBlueExpensePreviewState() = ExpenseListUiState(
    month = YearMonth(2026, 11), comparison = previewComparison(), totalAmountText = "84,500",
    expenseCount = 3, hasExpensesInMonth = true, isExpenseSourceReady = true,
    dayGroups = listOf(ExpenseDayUiModel(LocalDate(2026, 11, 25), "11월 25일", "84,500", listOf(
        ExpenseItemUiModel("sky-1", "동네 마트", "56,000", ExpenseCategory.VARIABLE, "계좌이체"),
        ExpenseItemUiModel("sky-2", "함께 먹은 저녁", "22,000", ExpenseCategory.VARIABLE, "현금"),
        ExpenseItemUiModel("sky-3", "동네 카페", "6,500", ExpenseCategory.VARIABLE, "현금"),
    ))),
)

@Preview(name = "스카이블루 · 지출과 이모지 비교", widthDp = 390, heightDp = 1000, locale = "ko")
@Composable
private fun SkyBlueExpensePreview() = WoorisseumTheme(darkTheme = false) {
    ExpenseListScreen(skyBlueExpensePreviewState(), {})
}

@Preview(name = "스카이블루 · 이모지 비교 그래프", widthDp = 390, locale = "ko")
@Composable
private fun EmojiComparisonPreview() = WoorisseumTheme(darkTheme = false) {
    androidx.compose.material3.Surface { ExpenseComparisonSection(previewComparison()) }
}

@Preview(name = "이모지 비교 · 320dp 큰 글씨", widthDp = 320, locale = "ko")
@Composable
private fun EmojiComparisonLargeFontPreview() = WoorisseumTheme(darkTheme = false) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.5f),
    ) {
        androidx.compose.material3.Surface { ExpenseComparisonSection(previewComparison()) }
    }
}
