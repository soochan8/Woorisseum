package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.feature.transaction.mapper.toFormUiState
import com.woorisseum.feature.transaction.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 원본과 입력을 분리한다. 저장 기능 연결 전에는 제출해도 원본 갱신·성공 이동을 하지 않는다. */
class ExpenseEditViewModel(expenseId: String, initialExpense: Expense? = null) : ViewModel() {
    init { require(initialExpense == null || initialExpense.id == expenseId) }
    private val originalForm = initialExpense?.toFormUiState()
    private val _uiState = MutableStateFlow(ExpenseEditUiState(expenseId, form = originalForm))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<ExpenseEditUiEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: ExpenseEditUiIntent) {
        when (intent) {
            is ExpenseEditUiIntent.FormChanged -> _uiState.update { state ->
                val form = state.form?.changed(intent.intent) ?: return@update state
                state.copy(form = form,
                    hasChanges = form.copy(amountError = null, paymentMethodRequired = false) != originalForm,
                    showStorageUnavailable = false)
            }
            ExpenseEditUiIntent.Submit -> _uiState.update { state ->
                if (!state.hasChanges) return@update state
                val form = state.form?.validated() ?: return@update state
                state.copy(form = form, showStorageUnavailable = !form.hasErrors)
            }
            ExpenseEditUiIntent.Cancel -> viewModelScope.launch {
                _effects.emit(ExpenseEditUiEffect.NavigateBack)
            }
        }
    }
}
