package com.woorisseum.feature.transaction.model

sealed interface ExpenseEntryUiIntent {
    data class FormChanged(val intent: ExpenseFormUiIntent) : ExpenseEntryUiIntent
    data object Submit : ExpenseEntryUiIntent
}
