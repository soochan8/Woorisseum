package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import kotlinx.datetime.LocalDate

/** null은 조회 기능 미연결이다. 존재하지 않는 내역이나 0원으로 대체하지 않는다. */
data class ExpenseDetailUiState(val expenseId: String, val detail: ExpenseDetailUiModel? = null)

data class ExpenseDetailUiModel(
    val amountText: String,
    val date: LocalDate,
    val merchant: String?,
    val category: ExpenseCategory,
    val paymentMethod: PaymentMethod,
    val memo: String?,
)

sealed interface ExpenseDetailUiIntent { data object Edit : ExpenseDetailUiIntent }
sealed interface ExpenseDetailUiEffect {
    data class NavigateToEdit(val expenseId: String) : ExpenseDetailUiEffect
}
