package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.core.domain.transaction.model.isValidExpenseAmount
import kotlinx.datetime.LocalDate

/** 등록·수정이 공유하는 입력값과 필드 검증 상태다. 저장 결과는 포함하지 않는다. */
data class ExpenseFormUiState(
    val date: LocalDate,
    val amountInput: String = "",
    val merchantInput: String = "",
    val memoInput: String = "",
    val category: ExpenseCategory = ExpenseCategory.VARIABLE,
    val paymentMethod: PaymentMethod? = null,
    val availablePaymentMethods: List<PaymentMethod> = listOf(PaymentMethod.Cash, PaymentMethod.BankTransfer),
    val amountError: ExpenseAmountInputError? = null,
    val paymentMethodRequired: Boolean = false,
    val cardLoadFailed: Boolean = false,
) {
    val hasErrors: Boolean get() = amountError != null || paymentMethodRequired
}

sealed interface ExpenseFormUiIntent {
    data object RetryCards : ExpenseFormUiIntent
    data class AmountChanged(val value: String) : ExpenseFormUiIntent
    data class MerchantChanged(val value: String) : ExpenseFormUiIntent
    data class MemoChanged(val value: String) : ExpenseFormUiIntent
    data class DateChanged(val date: LocalDate) : ExpenseFormUiIntent
    data class CategorySelected(val category: ExpenseCategory) : ExpenseFormUiIntent
    data class PaymentMethodSelected(val paymentMethod: PaymentMethod) : ExpenseFormUiIntent
}

/** 부수효과 없이 입력값만 전이한다. 제출·저장 정책은 각 ViewModel이 결정한다. */
internal fun ExpenseFormUiState.changed(intent: ExpenseFormUiIntent): ExpenseFormUiState = when (intent) {
    ExpenseFormUiIntent.RetryCards -> this
    is ExpenseFormUiIntent.AmountChanged -> copy(
        amountInput = intent.value,
        amountError = if (amountError == null) null else validateExpenseAmount(intent.value),
    )
    is ExpenseFormUiIntent.MerchantChanged -> copy(merchantInput = intent.value)
    is ExpenseFormUiIntent.MemoChanged -> copy(memoInput = intent.value)
    is ExpenseFormUiIntent.DateChanged -> copy(date = intent.date)
    is ExpenseFormUiIntent.CategorySelected -> copy(category = intent.category)
    is ExpenseFormUiIntent.PaymentMethodSelected -> if (intent.paymentMethod in availablePaymentMethods) {
        copy(paymentMethod = intent.paymentMethod, paymentMethodRequired = false)
    } else this
}

internal fun ExpenseFormUiState.validated() = copy(
    amountError = validateExpenseAmount(amountInput),
    paymentMethodRequired = paymentMethod == null,
)

internal fun validateExpenseAmount(input: String): ExpenseAmountInputError? {
    if (input.isBlank()) return ExpenseAmountInputError.REQUIRED
    // 부호·소수·문자를 제거해 사용자가 입력한 금액을 바꾸지 않는다.
    if (input.any { it !in '0'..'9' }) return ExpenseAmountInputError.INVALID_INTEGER
    val amount = input.toLongOrNull() ?: return ExpenseAmountInputError.OUT_OF_RANGE
    return if (isValidExpenseAmount(amount)) null else ExpenseAmountInputError.NOT_POSITIVE
}

enum class ExpenseAmountInputError { REQUIRED, INVALID_INTEGER, OUT_OF_RANGE, NOT_POSITIVE }
