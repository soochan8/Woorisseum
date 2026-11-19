package com.woorisseum.core.domain.transaction.search

import com.woorisseum.core.domain.transaction.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/** 검색어는 사용처·메모에 적용하고, 다른 조건은 모두 함께 만족해야 한다. */
data class ExpenseSearch(
    val month: YearMonth? = null,
    val query: String = "",
    val category: ExpenseCategory? = null,
    val start: LocalDate? = null,
    val endInclusive: LocalDate? = null,
    val paymentMethod: PaymentMethod? = null,
) {
    fun matches(expense: Expense): Boolean {
        val term = query.trim()
        return (month == null || expense.date.year == month.year && expense.date.month == month.month) &&
            (category == null || expense.category == category) &&
            (start == null || expense.date >= start) && (endInclusive == null || expense.date <= endInclusive) &&
            (paymentMethod == null || expense.paymentMethod.sameSelection(paymentMethod)) &&
            (term.isEmpty() || expense.merchant?.contains(term, ignoreCase = true) == true ||
                expense.memo?.contains(term, ignoreCase = true) == true)
    }
}

/** 표시명·카드사가 바뀌어도 카드 식별자로 조회한다. */
private fun PaymentMethod.sameSelection(other: PaymentMethod): Boolean = when {
    this is PaymentMethod.Card && other is PaymentMethod.Card -> cardId == other.cardId
    else -> this == other
}
