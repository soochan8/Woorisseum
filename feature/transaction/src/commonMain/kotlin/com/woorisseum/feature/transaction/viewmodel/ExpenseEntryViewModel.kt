package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import com.woorisseum.core.domain.transaction.model.isValidExpenseAmount
import com.woorisseum.feature.transaction.model.ExpenseAmountInputError
import com.woorisseum.feature.transaction.model.ExpenseEntryUiIntent
import com.woorisseum.feature.transaction.model.ExpenseEntryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/** 입력 전이를 소유한다. 저장소 연결 전에는 성공·화면 이동을 발생시키지 않는다. */
class ExpenseEntryViewModel(initialDate: LocalDate) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseEntryUiState(date = initialDate))
    val uiState = _uiState.asStateFlow()

    fun onIntent(intent: ExpenseEntryUiIntent) {
        _uiState.update { state ->
            // 입력을 바꾸면 이전 제출 안내를 지우고 새 입력을 계속 편집할 수 있다.
            val editing = state.copy(showStorageUnavailable = false)
            when (intent) {
                is ExpenseEntryUiIntent.AmountChanged -> editing.copy(
                    amountInput = intent.value,
                    amountError = if (state.amountError != null) validateAmount(intent.value) else null,
                )
                is ExpenseEntryUiIntent.MerchantChanged -> editing.copy(merchantInput = intent.value)
                is ExpenseEntryUiIntent.DateChanged -> editing.copy(date = intent.date)
                is ExpenseEntryUiIntent.CategorySelected -> editing.copy(category = intent.category)
                is ExpenseEntryUiIntent.PaymentMethodSelected -> editing.copy(
                    paymentMethod = intent.paymentMethod,
                    paymentMethodRequired = false,
                )
                ExpenseEntryUiIntent.Submit -> {
                    val amountError = validateAmount(state.amountInput)
                    val paymentMethodRequired = state.paymentMethod == null
                    state.copy(
                        amountError = amountError,
                        paymentMethodRequired = paymentMethodRequired,
                        showStorageUnavailable = amountError == null && !paymentMethodRequired,
                    )
                }
            }
        }
    }

    private fun validateAmount(input: String): ExpenseAmountInputError? {
        if (input.isBlank()) return ExpenseAmountInputError.REQUIRED
        // 부호·소수·문자 혼입을 조용히 제거해 다른 금액으로 바꾸지 않는다.
        if (input.any { it !in '0'..'9' }) return ExpenseAmountInputError.INVALID_INTEGER
        val amount = input.toLongOrNull() ?: return ExpenseAmountInputError.OUT_OF_RANGE
        return if (isValidExpenseAmount(amount)) null else ExpenseAmountInputError.NOT_POSITIVE
    }
}
