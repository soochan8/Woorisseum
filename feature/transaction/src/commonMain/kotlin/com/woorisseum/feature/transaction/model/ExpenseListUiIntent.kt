package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import kotlinx.datetime.LocalDate

/** Screen이 전달하고 ViewModel이 처리하는 사용자 동작이다. */
sealed interface ExpenseListUiIntent {
    data object PreviousMonth : ExpenseListUiIntent

    data object NextMonth : ExpenseListUiIntent

    /** null은 모든 분류를 표시하는 '전체' 선택이다. */
    data class SelectCategory(val category: ExpenseCategory?) : ExpenseListUiIntent

    data class SelectDateRange(val start: LocalDate, val endInclusive: LocalDate) : ExpenseListUiIntent

    data class SelectPaymentMethod(val paymentMethod: ExpensePaymentFilter?) : ExpenseListUiIntent

    data object ResetFilters : ExpenseListUiIntent

    data class OpenExpense(val expenseId: String) : ExpenseListUiIntent

    data object AddExpense : ExpenseListUiIntent
    data object ViewFixedExpenses : ExpenseListUiIntent
    data object ViewBudget : ExpenseListUiIntent
}
