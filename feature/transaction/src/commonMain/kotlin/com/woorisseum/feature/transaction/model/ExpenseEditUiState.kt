package com.woorisseum.feature.transaction.model

/** 조회한 원본과 입력 초안을 분리하며 실패해도 입력을 유지한다. */
data class ExpenseEditUiState(
    val expenseId: String,
    val form: ExpenseFormUiState? = null,
    val hasChanges: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: ExpenseOperationError? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val saveError: ExpenseOperationError? = null,
)

sealed interface ExpenseEditUiIntent {
    data class FormChanged(val intent: ExpenseFormUiIntent) : ExpenseEditUiIntent
    data object Reload : ExpenseEditUiIntent
    data object Submit : ExpenseEditUiIntent
    data object Cancel : ExpenseEditUiIntent
}
sealed interface ExpenseEditUiEffect { data object NavigateBack : ExpenseEditUiEffect }
