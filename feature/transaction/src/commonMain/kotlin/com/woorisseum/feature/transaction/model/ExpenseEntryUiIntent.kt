package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import kotlinx.datetime.LocalDate

sealed interface ExpenseEntryUiIntent {
    data class AmountChanged(val value: String) : ExpenseEntryUiIntent
    data class MerchantChanged(val value: String) : ExpenseEntryUiIntent
    data class DateChanged(val date: LocalDate) : ExpenseEntryUiIntent
    data class CategorySelected(val category: ExpenseCategory) : ExpenseEntryUiIntent
    data class PaymentMethodSelected(val paymentMethod: PaymentMethod) : ExpenseEntryUiIntent
    data object Submit : ExpenseEntryUiIntent
}
