package com.woorisseum.feature.transaction.model

/** Route가 앱의 이동 콜백으로 연결하는 일회성 요청이다. */
sealed interface ExpenseListUiEffect {
    data class NavigateToDetail(val expenseId: String) : ExpenseListUiEffect
    data object NavigateToAddExpense : ExpenseListUiEffect
}
