package com.woorisseum.core.domain.transaction.fixed

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/** 시작월 이후의 매월 계획을 투영하고 합산한다. 없는 결제일은 해당 월의 말일로 조정한다. */
class CalculateMonthlyFixedExpensesUseCase {
    operator fun invoke(
        month: YearMonth,
        plans: List<FixedExpensePlan>,
        paidPlanIds: Set<String>? = null,
    ): MonthlyFixedExpenseSummary {
        require(plans.map { it.id }.distinct().size == plans.size) { "Duplicate fixed expense identifiers" }
        var total = 0L
        var paid = 0L
        val items = plans.filter { month >= it.startMonth }.map { plan ->
            require(plan.amount <= Long.MAX_VALUE - total) { "Fixed expense total exceeds Long range" }
            total += plan.amount
            val status = when {
                paidPlanIds == null -> FixedExpensePaymentStatus.UNKNOWN
                plan.id in paidPlanIds -> FixedExpensePaymentStatus.PAID
                else -> FixedExpensePaymentStatus.UNPAID
            }
            if (status == FixedExpensePaymentStatus.PAID) paid += plan.amount
            ScheduledFixedExpense(plan,
                LocalDate(month.year, month.month, plan.billingDay.coerceAtMost(month.numberOfDays)), status)
        }.sortedWith(compareBy({ it.dueDate }, { it.plan.id }))
        return MonthlyFixedExpenseSummary(items, total, if (paidPlanIds == null) null else paid)
    }
}
