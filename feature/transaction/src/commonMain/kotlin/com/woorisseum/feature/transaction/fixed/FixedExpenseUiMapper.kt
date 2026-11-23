package com.woorisseum.feature.transaction.fixed

import com.woorisseum.core.domain.transaction.fixed.FixedExpensePlan
import com.woorisseum.core.domain.transaction.fixed.MonthlyFixedExpenseSummary

/** 표시에 필요한 금액 형식만 변환한다. 반복일·납부 상태 계산은 domain이 담당한다. */
internal fun MonthlyFixedExpenseSummary.toUiModel() = FixedExpenseSummaryUiModel(
    totalAmountText = totalAmount?.fixedAmountText().orEmpty(), totalOverflow = totalOverflow, paidAmountText = paidAmount?.fixedAmountText(),
    items = items.map { FixedExpenseItemUiModel(it.plan.id, it.plan.type, it.plan.name?.takeIf(String::isNotBlank),
        it.plan.amount.fixedAmountText(), it.dueDate, it.paymentStatus) },
)
internal fun FixedExpensePlan.toFormUiState() = FixedExpenseFormUiState(
    startMonth = startMonth, type = type, nameInput = name.orEmpty(), amountInput = amount.toString(),
    billingDayInput = billingDay.toString(), memoInput = memo.orEmpty(),
)
private fun Long.fixedAmountText(): String = toString().reversed().chunked(3).joinToString(",").reversed()

/** 검증된 폼을 저장 명령의 입력값으로 변환한다. */
internal fun FixedExpenseFormUiState.toInput() = com.woorisseum.core.domain.transaction.fixed.FixedExpenseInput(
    type = requireNotNull(type), name = nameInput.trim().takeIf { it.isNotEmpty() },
    amount = amountInput.toLong(), billingDay = billingDayInput.toInt(),
    memo = memoInput.trim().takeIf { it.isNotEmpty() },
)
