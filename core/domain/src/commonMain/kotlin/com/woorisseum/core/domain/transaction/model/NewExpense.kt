package com.woorisseum.core.domain.transaction.model

import kotlinx.datetime.LocalDate

/** 새 지출 등록에 필요한 값이다. 입력창 문자열 대신 변환된 값을 받고, 저장 전이므로 식별자는 없다. */
data class NewExpense(
    val amount: Long,
    val date: LocalDate,
    val merchant: String?,
    val category: ExpenseCategory,
    val paymentMethod: PaymentMethod,
)
