package com.woorisseum.feature.transaction.fixed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.transaction.fixed.CalculateMonthlyFixedExpensesUseCase
import com.woorisseum.core.domain.transaction.fixed.FixedExpensePlan
import com.woorisseum.core.domain.transaction.fixed.FixedExpenseType
import com.woorisseum.feature.transaction.model.validateExpenseAmount
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth

/** 월 선택·화면 이동을 담당한다. 초기 계획이 없으면 조회 미연결 상태를 유지한다. */
class FixedExpenseListViewModel(
    initialMonth: YearMonth,
    private val calculateMonthly: CalculateMonthlyFixedExpensesUseCase,
    initialPlans: List<FixedExpensePlan>? = null,
) : ViewModel() {
    private val plans = initialPlans?.toList()
    private val _uiState = MutableStateFlow(stateForMonth(initialMonth))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<FixedExpenseListUiEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: FixedExpenseListUiIntent) {
        when (intent) {
            FixedExpenseListUiIntent.PreviousMonth -> _uiState.value = stateForMonth(_uiState.value.month.minusMonth())
            FixedExpenseListUiIntent.NextMonth -> _uiState.value = stateForMonth(_uiState.value.month.plusMonth())
            FixedExpenseListUiIntent.AddPlan -> openEditor()
            is FixedExpenseListUiIntent.EditPlan -> if (_uiState.value.summary?.items?.any { it.id == intent.id } == true) {
                openEditor(intent.id)
            }
        }
    }
    private fun openEditor(id: String? = null) {
        val month = _uiState.value.month
        viewModelScope.launch { _effects.emit(FixedExpenseListUiEffect.OpenEditor(month, id)) }
    }
    private fun stateForMonth(month: YearMonth) = FixedExpenseListUiState(month,
        plans?.let { calculateMonthly(month, it).toUiModel() })
}
