package com.woorisseum.feature.transaction.model

/** 등록 화면이 입력 폼과 제출 안내를 소유한다. */
data class ExpenseEntryUiState(
    val form: ExpenseFormUiState,
    val showStorageUnavailable: Boolean = false,
)
