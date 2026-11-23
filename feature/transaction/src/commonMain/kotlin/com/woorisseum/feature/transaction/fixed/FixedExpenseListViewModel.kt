package com.woorisseum.feature.transaction.fixed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.fixed.CalculateMonthlyFixedExpensesUseCase
import com.woorisseum.core.domain.transaction.fixed.FixedExpenseRepository
import com.woorisseum.feature.transaction.model.toExpenseOperationError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth

/** 선택 월에 유효한 계획을 관찰한다. 납부 상태를 실제 납부로 추정하지 않는다. */
class FixedExpenseListViewModel(
    initialMonth: YearMonth,
    private val calculateMonthly: CalculateMonthlyFixedExpensesUseCase,
    private val repository: FixedExpenseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FixedExpenseListUiState(initialMonth, isLoading = true))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<FixedExpenseListUiEffect>()
    val effects = _effects.asSharedFlow()
    private var observation: Job? = null

    init { observeMonth(initialMonth) }

    private fun observeMonth(month: YearMonth) {
        observation?.cancel()
        _uiState.value = FixedExpenseListUiState(month, isLoading = true)
        observation = viewModelScope.launch {
            repository.observePlans(month).collect { result ->
                if (_uiState.value.month != month) return@collect
                _uiState.value = when (result) {
                    is AppResult.Success -> FixedExpenseListUiState(month, calculateMonthly(month, result.data).toUiModel())
                    is AppResult.Failure -> FixedExpenseListUiState(month, loadError = result.error.toExpenseOperationError())
                }
            }
        }
    }

    fun onIntent(intent: FixedExpenseListUiIntent) {
        when (intent) {
            is FixedExpenseListUiIntent.ShowMonth -> observeMonth(intent.month)
            FixedExpenseListUiIntent.Retry -> observeMonth(_uiState.value.month)
            FixedExpenseListUiIntent.PreviousMonth -> observeMonth(_uiState.value.month.minusMonth())
            FixedExpenseListUiIntent.NextMonth -> observeMonth(_uiState.value.month.plusMonth())
            FixedExpenseListUiIntent.AddPlan -> openEditor()
            is FixedExpenseListUiIntent.EditPlan -> if (_uiState.value.summary?.items?.any { it.id == intent.id } == true) openEditor(intent.id)
        }
    }

    private fun openEditor(id: String? = null) {
        val month = _uiState.value.month
        viewModelScope.launch { _effects.emit(FixedExpenseListUiEffect.OpenEditor(month, id)) }
    }
}
