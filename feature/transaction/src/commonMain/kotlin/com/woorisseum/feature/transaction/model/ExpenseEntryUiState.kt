package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import kotlinx.datetime.LocalDate

/** 입력 중인 문자열과 선택값을 보관한다. 저장된 지출과는 별개의 화면 상태다. */
data class ExpenseEntryUiState(
    val date: LocalDate,
    val amountInput: String = "",
    val merchantInput: String = "",
    val category: ExpenseCategory = ExpenseCategory.VARIABLE,
    val paymentMethod: PaymentMethod? = null,
    val amountError: ExpenseAmountInputError? = null,
    val paymentMethodRequired: Boolean = false,
    val showStorageUnavailable: Boolean = false,
)

enum class ExpenseAmountInputError {
    REQUIRED,
    INVALID_INTEGER,
    OUT_OF_RANGE,
    NOT_POSITIVE,
}
