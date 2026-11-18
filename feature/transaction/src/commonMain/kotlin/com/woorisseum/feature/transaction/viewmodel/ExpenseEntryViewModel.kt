package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import com.woorisseum.feature.transaction.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/** 입력 전이를 소유한다. 저장소 연결 전에는 성공·화면 이동을 발생시키지 않는다. */
class ExpenseEntryViewModel(initialDate: LocalDate) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseEntryUiState(ExpenseFormUiState(date = initialDate)))
    val uiState = _uiState.asStateFlow()

    fun onIntent(intent: ExpenseEntryUiIntent) {
        _uiState.update { state ->
            when (intent) {
                is ExpenseEntryUiIntent.FormChanged -> state.copy(
                    form = state.form.changed(intent.intent), showStorageUnavailable = false,
                )
                ExpenseEntryUiIntent.Submit -> {
                    val form = state.form.validated()
                    state.copy(form = form, showStorageUnavailable = !form.hasErrors)
                }
            }
        }
    }
}
