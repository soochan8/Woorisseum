package com.woorisseum.feature.transaction.mapper

import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.feature.transaction.model.ExpenseDetailUiModel
import com.woorisseum.feature.transaction.model.ExpenseFormUiState

/** 도메인 값을 표시 형식으로만 변환한다. 조회·상태 전이는 수행하지 않는다. */
fun Expense.toDetailUiModel() = ExpenseDetailUiModel(
    amountText = amount.toString().reversed().chunked(3).joinToString(",").reversed(),
    date = date,
    merchant = merchant?.takeIf { it.isNotBlank() },
    category = category,
    paymentMethod = paymentMethod,
    memo = memo?.takeIf { it.isNotBlank() },
)

/** 수정 폼의 초기값이다. 원본 지출은 수정하지 않는다. */
internal fun Expense.toFormUiState() = ExpenseFormUiState(
    date = date, amountInput = amount.toString(), merchantInput = merchant.orEmpty(),
    memoInput = memo.orEmpty(), category = category, paymentMethod = paymentMethod,
    // 신규 카드 목록을 만들지 않고 원본의 카드만 유지해 다시 선택할 수 있게 한다.
    availablePaymentMethods = listOf(PaymentMethod.Cash, PaymentMethod.BankTransfer) +
        listOfNotNull(paymentMethod as? PaymentMethod.Card),
)
