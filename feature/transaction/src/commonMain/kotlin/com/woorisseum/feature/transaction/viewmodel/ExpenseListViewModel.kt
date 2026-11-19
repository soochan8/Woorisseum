package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.feature.transaction.model.ExpenseListUiEffect
import com.woorisseum.feature.transaction.model.ExpenseListUiIntent
import com.woorisseum.feature.transaction.model.ExpenseListUiState
import com.woorisseum.feature.transaction.model.ExpenseListFilters
import com.woorisseum.feature.transaction.model.ExpenseDateRange
import com.woorisseum.feature.transaction.model.toExpensePaymentFilter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.number
import kotlinx.datetime.plusMonth

/** 월·분류 선택과 이동 요청을 처리한다. 데이터 조회는 Repository 연결 단계에서 추가한다. */
class ExpenseListViewModel(initialMonth: YearMonth) : ViewModel() {
    private val _uiState = MutableStateFlow(emptyMonthState(initialMonth))
    val uiState = _uiState.asStateFlow()

    // 화면이 비활성 상태일 때 발생한 이동 요청은 재생하지 않는다.
    private val _effects = MutableSharedFlow<ExpenseListUiEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: ExpenseListUiIntent) {
        when (intent) {
            ExpenseListUiIntent.PreviousMonth -> _uiState.update {
                emptyMonthState(it.month.minusMonth(), it)
            }
            ExpenseListUiIntent.NextMonth -> _uiState.update {
                emptyMonthState(it.month.plusMonth(), it)
            }
            is ExpenseListUiIntent.SelectCategory -> _uiState.update {
                it.copy(filters = it.filters.copy(category = intent.category))
            }
            is ExpenseListUiIntent.SelectDateRange -> _uiState.update { state ->
                if (intent.start > intent.endInclusive || intent.start < state.month.firstDay ||
                    intent.endInclusive > state.month.lastDay
                ) state else {
                    // 월 전체를 선택하면 별도 날짜 필터가 없는 상태로 정규화한다.
                    val range = if (intent.start == state.month.firstDay && intent.endInclusive == state.month.lastDay) {
                        null
                    } else ExpenseDateRange(intent.start, intent.endInclusive)
                    state.copy(filters = state.filters.copy(dateRange = range))
                }
            }
            is ExpenseListUiIntent.SelectPaymentMethod -> _uiState.update { state ->
                if (intent.paymentMethod == null || state.availablePaymentMethods.any {
                    it.toExpensePaymentFilter() == intent.paymentMethod
                }) state.copy(filters = state.filters.copy(paymentMethod = intent.paymentMethod))
                else state
            }
            ExpenseListUiIntent.ResetFilters -> _uiState.update {
                it.copy(filters = ExpenseListFilters())
            }
            is ExpenseListUiIntent.OpenExpense -> viewModelScope.launch {
                _effects.emit(ExpenseListUiEffect.NavigateToDetail(intent.expenseId))
            }
            ExpenseListUiIntent.AddExpense -> viewModelScope.launch {
                _effects.emit(ExpenseListUiEffect.NavigateToAddExpense)
            }
            ExpenseListUiIntent.ViewFixedExpenses -> {
                val month = _uiState.value.month
                viewModelScope.launch { _effects.emit(ExpenseListUiEffect.NavigateToFixedExpenses(month)) }
            }
            ExpenseListUiIntent.ViewBudget -> {
                val month = _uiState.value.month
                viewModelScope.launch { _effects.emit(ExpenseListUiEffect.NavigateToBudget(month)) }
            }
        }
    }

    private fun emptyMonthState(
        month: YearMonth,
        previousState: ExpenseListUiState? = null,
    ): ExpenseListUiState {
        val state = ExpenseListUiState(
            month = month,
            periodText = "${month.month.number}.1 – ${month.month.number}.${month.numberOfDays}",
            filters = previousState?.filters?.copy(dateRange = null) ?: ExpenseListFilters(),
        )
        return if (previousState == null) state else state.copy(
            availablePaymentMethods = previousState.availablePaymentMethods,
        )
    }
}
