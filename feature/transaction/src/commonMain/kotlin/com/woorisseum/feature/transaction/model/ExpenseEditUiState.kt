package com.woorisseum.feature.transaction.model

/** 원본을 불러올 수 있을 때만 수정 폼을 노출한다. 제출 안내는 저장 성공을 의미하지 않는다. */
data class ExpenseEditUiState(
    val expenseId: String,
    val form: ExpenseFormUiState? = null,
    val hasChanges: Boolean = false,
    val showStorageUnavailable: Boolean = false,
)

sealed interface ExpenseEditUiIntent {
    data class FormChanged(val intent: ExpenseFormUiIntent) : ExpenseEditUiIntent
    data object Submit : ExpenseEditUiIntent
    data object Cancel : ExpenseEditUiIntent
}
sealed interface ExpenseEditUiEffect { data object NavigateBack : ExpenseEditUiEffect }
