package com.woorisseum.feature.budget.mapper

import com.woorisseum.core.domain.budget.model.BudgetSummary
import com.woorisseum.feature.budget.model.BudgetCategoryUiModel
import com.woorisseum.feature.budget.model.BudgetHeadline
import com.woorisseum.feature.budget.model.BudgetSummaryUiModel
import com.woorisseum.feature.budget.model.BudgetUsage
import kotlin.math.roundToLong

/** 계산된 값을 표시 형식으로 변환한다. 상태 변경이나 업무 계산을 하지 않는다. */
fun BudgetSummary.toUiModel(): BudgetSummaryUiModel {
    val headline = when {
        totalOverflow -> BudgetHeadline.OVERFLOW
        budgetAmount == null -> BudgetHeadline.UNSET
        spentAmount == null -> BudgetHeadline.WAITING
        remainingAmount?.let { it < 0L } == true -> BudgetHeadline.EXCEEDED
        budgetAmount == 0L -> BudgetHeadline.ZERO
        else -> BudgetHeadline.REMAINING
    }
    val usage = when {
        totalOverflow -> BudgetUsage.OVERFLOW
        budgetAmount == null -> BudgetUsage.UNSET
        budgetAmount == 0L -> BudgetUsage.ZERO_BUDGET
        spentAmount == null -> BudgetUsage.WAITING
        else -> BudgetUsage.AVAILABLE
    }
    return BudgetSummaryUiModel(
        totalOverflow = totalOverflow,
        budgetAmountText = budgetAmount?.formatBudgetAmount(),
        spentAmountText = spentAmount?.formatBudgetAmount(),
        headlineAmountText = remainingAmount?.let {
            (if (it < 0L) -it else it).formatBudgetAmount()
        },
        headline = headline,
        usage = usage,
        usagePercentText = usageRatio?.let { (it.coerceAtMost(9.99) * 100).roundToLong().toString() },
        isUsageAboveDisplayLimit = usageRatio?.let { it > 9.99 } == true,
        progress = usageRatio?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f,
        categories = categories.map {
            BudgetCategoryUiModel(
                category = it.category,
                amountOverflow = totalOverflow && it.amount == null,
                amountText = it.amount?.formatBudgetAmount(),
                sharePercentText = it.share?.let { share -> (share * 100).roundToLong().toString() },
                progress = it.share?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f,
            )
        },
    )
}

internal fun Long.formatBudgetAmount(): String = toString().reversed().chunked(3).joinToString(",").reversed()
