package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import kotlinx.datetime.LocalDate

/** 로딩·항목 없음·조회 실패를 구분하며 가짜 0원 내역을 표시하지 않는다. */
data class ExpenseDetailUiState(
    val expenseId: String, val detail: ExpenseDetailUiModel? = null,
    val isLoading: Boolean = false, val loadError: ExpenseOperationError? = null,
)

data class ExpenseDetailUiModel(
    val amountText: String,
    val date: LocalDate,
    val merchant: String?,
    val category: ExpenseCategory,
    val paymentMethod: PaymentMethod,
    val memo: String?,
)

sealed interface ExpenseDetailUiIntent {
    data object Refresh : ExpenseDetailUiIntent
    data object Edit : ExpenseDetailUiIntent
}
sealed interface ExpenseDetailUiEffect {
    data class NavigateToEdit(val expenseId: String) : ExpenseDetailUiEffect
}
