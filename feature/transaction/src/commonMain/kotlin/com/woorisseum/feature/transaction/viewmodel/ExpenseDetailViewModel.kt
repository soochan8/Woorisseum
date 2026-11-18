package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.feature.transaction.mapper.toDetailUiModel
import com.woorisseum.feature.transaction.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 상세 표시와 수정 이동을 담당한다. 실제 조회는 후속 Repository 연결 범위다. */
class ExpenseDetailViewModel(expenseId: String, initialExpense: Expense? = null) : ViewModel() {
    init { require(initialExpense == null || initialExpense.id == expenseId) }
    // 초기 내역이 없으면 조회 미연결 상태다. 앱 DI에서는 예시 내역을 주입하지 않는다.
    private val _uiState = MutableStateFlow(ExpenseDetailUiState(expenseId, initialExpense?.toDetailUiModel()))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<ExpenseDetailUiEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: ExpenseDetailUiIntent) {
        when (intent) {
            ExpenseDetailUiIntent.Edit -> if (_uiState.value.detail != null) {
                val id = _uiState.value.expenseId
                viewModelScope.launch { _effects.emit(ExpenseDetailUiEffect.NavigateToEdit(id)) }
            }
        }
    }
}
