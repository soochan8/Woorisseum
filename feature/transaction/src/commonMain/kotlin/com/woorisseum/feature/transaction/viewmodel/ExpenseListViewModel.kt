package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.feature.transaction.model.ExpenseListUiEffect
import com.woorisseum.feature.transaction.model.ExpenseListUiIntent
import com.woorisseum.feature.transaction.model.ExpenseListUiState
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
                it.copy(selectedCategory = intent.category)
            }
            ExpenseListUiIntent.AddExpense -> viewModelScope.launch {
                _effects.emit(ExpenseListUiEffect.NavigateToAddExpense)
            }
        }
    }

    private fun emptyMonthState(
        month: YearMonth,
        previousState: ExpenseListUiState? = null,
    ): ExpenseListUiState = ExpenseListUiState(
        month = month,
        periodText = "${month.month.number}.1 – ${month.month.number}.${month.numberOfDays}",
        selectedCategory = previousState?.selectedCategory,
    )
}
