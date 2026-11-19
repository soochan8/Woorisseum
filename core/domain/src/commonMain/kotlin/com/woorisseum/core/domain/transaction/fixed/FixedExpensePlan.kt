package com.woorisseum.core.domain.transaction.fixed

import com.woorisseum.core.domain.transaction.model.isValidExpenseAmount
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/** 반복 납부 계획이다. 실제 지출 내역과 납부 기록을 생성하지 않는다. */
data class FixedExpensePlan(
    val id: String,
    val type: FixedExpenseType,
    val name: String?,
    val amount: Long,
    val billingDay: Int,
    val startMonth: YearMonth,
    val memo: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(isValidExpenseAmount(amount))
        require(billingDay in 1..31)
        require(type != FixedExpenseType.OTHER || !name.isNullOrBlank())
    }
}

/** 지출의 고정비/변동비 분류와는 별개인 반복 항목의 종류다. */
enum class FixedExpenseType { RENT, TELECOMMUNICATION, SUBSCRIPTION, INSURANCE, UTILITIES, OTHER }
enum class FixedExpensePaymentStatus { PAID, UNPAID, UNKNOWN }

data class ScheduledFixedExpense(
    val plan: FixedExpensePlan,
    val dueDate: LocalDate,
    val paymentStatus: FixedExpensePaymentStatus,
)

data class MonthlyFixedExpenseSummary(
    val items: List<ScheduledFixedExpense>,
    val totalAmount: Long,
    // 납부 기록을 조회하지 못한 경우 0원으로 대체하지 않는다.
    val paidAmount: Long?,
)
