package com.woorisseum.core.domain.budget.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory

/** 미설정 예산과 미집계 지출을 0원과 구분한다. 비율은 0~1 단위이며 초과 시 1보다 클 수 있다. */
data class BudgetSummary(
    val budgetAmount: Long?,
    val spentAmount: Long?,
    val remainingAmount: Long?,
    val usageRatio: Double?,
    val categories: List<BudgetCategorySummary>,
)

data class BudgetCategorySummary(
    val category: ExpenseCategory,
    val amount: Long?,
    val share: Double?,
)
