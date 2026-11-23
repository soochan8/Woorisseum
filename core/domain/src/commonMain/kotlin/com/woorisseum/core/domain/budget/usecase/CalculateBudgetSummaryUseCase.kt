package com.woorisseum.core.domain.budget.usecase

import com.woorisseum.core.domain.budget.model.BudgetCategorySummary
import com.woorisseum.core.domain.budget.model.BudgetSummary
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.Expense

/** 전달된 분류별 합계로 예산 현황을 계산한다. 거래 조회·저장·화면 상태는 담당하지 않는다. */
class CalculateBudgetSummaryUseCase {
    /** 지출 합계의 표현 범위를 확인하고 계산한다. 조회·화면 로직을 섞지 않는다. */
    fun fromExpenses(budgetAmount: Long?, expenses: List<Expense>): BudgetSummary {
        require(budgetAmount == null || budgetAmount >= 0L)
        val totals = ExpenseCategory.entries.associateWith { 0L }.toMutableMap()
        val overflowed = mutableSetOf<ExpenseCategory>()
        var total = 0L
        var totalOverflow = false
        for (expense in expenses) {
            require(expense.amount >= 0L)
            val categoryTotal = totals.getValue(expense.category)
            if (expense.amount > Long.MAX_VALUE - categoryTotal) overflowed += expense.category
            else if (expense.category !in overflowed) totals[expense.category] = categoryTotal + expense.amount
            if (expense.amount > Long.MAX_VALUE - total) totalOverflow = true
            else if (!totalOverflow) total += expense.amount
        }
        if (!totalOverflow) return invoke(budgetAmount, totals)
        return BudgetSummary(budgetAmount, null, null, null,
            ExpenseCategory.entries.map { category ->
                BudgetCategorySummary(category, totals[category].takeUnless { category in overflowed }, null)
            },
            totalOverflow = true)
    }

    operator fun invoke(
        budgetAmount: Long?,
        categoryTotals: Map<ExpenseCategory, Long>?,
    ): BudgetSummary {
        require(budgetAmount == null || budgetAmount >= 0L)
        if (categoryTotals == null) return BudgetSummary(
            budgetAmount = budgetAmount,
            spentAmount = null,
            remainingAmount = null,
            usageRatio = null,
            categories = ExpenseCategory.entries.map { BudgetCategorySummary(it, null, null) },
        )
        var total = 0L
        categoryTotals.values.forEach { amount ->
            require(amount >= 0L)
            require(amount <= Long.MAX_VALUE - total) { "Expense total exceeds Long range" }
            total += amount
        }
        return BudgetSummary(
            budgetAmount = budgetAmount,
            spentAmount = total,
            remainingAmount = budgetAmount?.minus(total),
            usageRatio = budgetAmount?.takeIf { it > 0L }?.let { total.toDouble() / it.toDouble() },
            categories = ExpenseCategory.entries.map { category ->
                val amount = categoryTotals[category] ?: 0L
                BudgetCategorySummary(category, amount, if (total == 0L) 0.0 else amount.toDouble() / total)
            },
        )
    }
}
