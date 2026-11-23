package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.repository.ExpenseRepository
import com.woorisseum.feature.transaction.mapper.toDetailUiModel
import com.woorisseum.feature.transaction.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Route가 화면 활성화 때 조회를 요청해 수정 후 복귀한 상세도 갱신한다. */
class ExpenseDetailViewModel(private val expenseId: String, private val repository: ExpenseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseDetailUiState(expenseId, isLoading = true))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<ExpenseDetailUiEffect>()
    val effects = _effects.asSharedFlow()
    private var loading: Job? = null

    fun onIntent(intent: ExpenseDetailUiIntent) {
        when (intent) {
            ExpenseDetailUiIntent.Refresh -> {
                loading?.cancel()
                _uiState.update { it.copy(isLoading = true, loadError = null) }
                loading = viewModelScope.launch {
                    _uiState.value = when (val result = repository.getExpense(expenseId)) {
                        is AppResult.Success -> ExpenseDetailUiState(expenseId, result.data.toDetailUiModel())
                        is AppResult.Failure -> ExpenseDetailUiState(expenseId, loadError = result.error.toExpenseOperationError())
                    }
                }
            }
            ExpenseDetailUiIntent.Edit -> if (!_uiState.value.isLoading && _uiState.value.detail != null) {
                viewModelScope.launch { _effects.emit(ExpenseDetailUiEffect.NavigateToEdit(expenseId)) }
            }
        }
    }
}
