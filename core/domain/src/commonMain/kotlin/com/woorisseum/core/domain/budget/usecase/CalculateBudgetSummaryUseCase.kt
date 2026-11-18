package com.woorisseum.core.domain.budget.usecase

import com.woorisseum.core.domain.budget.model.BudgetCategorySummary
import com.woorisseum.core.domain.budget.model.BudgetSummary
import com.woorisseum.core.domain.transaction.model.ExpenseCategory

/** 전달된 분류별 합계로 예산 현황을 계산한다. 거래 조회·저장·화면 상태는 담당하지 않는다. */
class CalculateBudgetSummaryUseCase {
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
