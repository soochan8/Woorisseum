package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import kotlinx.datetime.LocalDate

/** null은 해당 조건의 제한이 없음을 뜻한다. 표시 문구와 조회 조건을 분리한다. */
data class ExpenseListFilters(
    val category: ExpenseCategory? = null,
    val dateRange: ExpenseDateRange? = null,
    val paymentMethod: ExpensePaymentFilter? = null,
) {
    val isActive: Boolean get() = category != null || dateRange != null || paymentMethod != null
}

/** 시작일·종료일을 모두 포함하는 날짜 범위다. */
data class ExpenseDateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(start <= endInclusive) }
}

/** 카드명 변경·동명 카드와 무관하게 식별자로 선택을 유지한다. */
sealed interface ExpensePaymentFilter {
    data object Cash : ExpensePaymentFilter
    data object BankTransfer : ExpensePaymentFilter
    data class Card(val cardId: String) : ExpensePaymentFilter
}

fun PaymentMethod.toExpensePaymentFilter(): ExpensePaymentFilter = when (this) {
    PaymentMethod.Cash -> ExpensePaymentFilter.Cash
    PaymentMethod.BankTransfer -> ExpensePaymentFilter.BankTransfer
    is PaymentMethod.Card -> ExpensePaymentFilter.Card(cardId)
}
