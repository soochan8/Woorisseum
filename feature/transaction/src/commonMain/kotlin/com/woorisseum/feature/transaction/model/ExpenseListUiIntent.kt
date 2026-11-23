package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/** Screen이 전달하고 ViewModel이 처리하는 사용자 동작이다. */
sealed interface ExpenseListUiIntent {
    data object RetryExpenses : ExpenseListUiIntent
    data object RetryCards : ExpenseListUiIntent
    data object OpenSearch : ExpenseListUiIntent
    data object CloseSearch : ExpenseListUiIntent
    data class SearchQueryChanged(val query: String) : ExpenseListUiIntent
    data object ClearSearch : ExpenseListUiIntent
    data class SelectSearchPreset(val preset: ExpenseSearchPreset) : ExpenseListUiIntent
    data class SelectSearchRange(val start: LocalDate, val endInclusive: LocalDate) : ExpenseListUiIntent
    data object PreviousMonth : ExpenseListUiIntent

    data object NextMonth : ExpenseListUiIntent

    /** null은 모든 분류를 표시하는 '전체' 선택이다. */
    data class SelectCategory(val category: ExpenseCategory?) : ExpenseListUiIntent

    data class SelectDateRange(val start: LocalDate, val endInclusive: LocalDate) : ExpenseListUiIntent

    data class SelectPaymentMethod(val paymentMethod: ExpensePaymentFilter?) : ExpenseListUiIntent

    data object ResetFilters : ExpenseListUiIntent

    data class OpenExpense(val expenseId: String) : ExpenseListUiIntent

    data object AddExpense : ExpenseListUiIntent
    /** 예산에서 요청한 월의 전체 내역을 연다. 일반 탭 전환은 이 Intent를 보내지 않는다. */
    data class ShowMonth(val month: YearMonth) : ExpenseListUiIntent
}
