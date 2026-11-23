package com.woorisseum.feature.transaction.model

/** 저장 성공은 State에 남겨 화면이 다시 활성화되어도 처리할 수 있다. */
data class ExpenseEntryUiState(
    val form: ExpenseFormUiState,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val saveError: ExpenseOperationError? = null,
)
