package com.woorisseum.feature.transaction.viewmodel

import com.woorisseum.core.domain.transaction.model.*
import com.woorisseum.core.domain.transaction.search.ExpenseSearch
import com.woorisseum.feature.transaction.model.*

/** 원본에서 조건별 목록과 날짜 그룹을 다시 구성한다. 검색 결과를 원본으로 재검색하지 않는다. */
internal fun ExpenseListUiState.withExpenseResults(expenses: List<Expense>): ExpenseListUiState {
    val monthItems = expenses.filter { it.date.year == month.year && it.date.month == month.month }
    val payment = when (val selected = filters.paymentMethod) {
        null -> null
        ExpensePaymentFilter.Cash -> PaymentMethod.Cash
        ExpensePaymentFilter.BankTransfer -> PaymentMethod.BankTransfer
        is ExpensePaymentFilter.Card -> PaymentMethod.Card(selected.cardId, "")
    }
    val range = if (isSearchOpen) searchPeriod.range else filters.dateRange
    val criteria = ExpenseSearch(if (isSearchOpen) null else month, if (isSearchOpen) searchQuery else "",
        filters.category, range?.start, range?.endInclusive, payment)
    val visible = expenses.filter(criteria::matches).sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })
    val monthTotal = monthItems.totalOrNull()
    return copy(
        isExpenseSourceReady = true,
        totalAmountText = monthTotal?.amountText().orEmpty(), totalAmountOverflow = monthTotal == null, expenseCount = monthItems.size,
        hasExpensesInMonth = monthItems.isNotEmpty(),
        dayGroups = visible.groupBy { it.date }.map { (date, items) ->
            val dayTotal = items.totalOrNull()
            ExpenseDayUiModel(date, "", dayTotal?.amountText().orEmpty(), items.map { item ->
                ExpenseItemUiModel(item.id, item.merchant.orEmpty(), item.amount.amountText(), item.category,
                    paymentMethodName = "", memo = item.memo, paymentMethod = item.paymentMethod)
            }, totalAmountOverflow = dayTotal == null)
        },
    )
}

private fun Long.amountText(): String = toString().reversed().chunked(3).joinToString(",").reversed()

/** 합계만 표현 범위를 초과해도 개별 검색 결과와 금액은 유지한다. */
private fun List<Expense>.totalOrNull(): Long? {
    var total = 0L
    for (expense in this) {
        val amount = expense.amount
        if (amount > 0L && total > Long.MAX_VALUE - amount || amount < 0L && total < Long.MIN_VALUE - amount) return null
        total += amount
    }
    return total
}
