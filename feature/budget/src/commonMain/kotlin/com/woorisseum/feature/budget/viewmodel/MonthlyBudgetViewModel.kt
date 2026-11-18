package com.woorisseum.feature.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.budget.usecase.CalculateBudgetSummaryUseCase
import com.woorisseum.feature.budget.mapper.toUiModel
import com.woorisseum.feature.budget.model.BudgetAmountInputError
import com.woorisseum.feature.budget.model.BudgetEditorUiState
import com.woorisseum.feature.budget.model.MonthlyBudgetUiEffect
import com.woorisseum.feature.budget.model.MonthlyBudgetUiIntent
import com.woorisseum.feature.budget.model.MonthlyBudgetUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth

/** 월별 화면 입력만 보관한다. 영속 저장·공유·실제 지출 조회는 후속 연동 범위다. */
class MonthlyBudgetViewModel(
    initialMonth: YearMonth,
    private val calculateSummary: CalculateBudgetSummaryUseCase,
) : ViewModel() {
    private val draftBudgets = mutableMapOf<YearMonth, Long>()
    private val _uiState = MutableStateFlow(stateForMonth(initialMonth))
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MonthlyBudgetUiEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: MonthlyBudgetUiIntent) {
        when (intent) {
            MonthlyBudgetUiIntent.PreviousMonth -> _uiState.value = stateForMonth(_uiState.value.month.minusMonth())
            MonthlyBudgetUiIntent.NextMonth -> _uiState.value = stateForMonth(_uiState.value.month.plusMonth())
            MonthlyBudgetUiIntent.EditBudget -> _uiState.update {
                it.copy(editor = BudgetEditorUiState(draftBudgets[it.month]?.toString().orEmpty()))
            }
            is MonthlyBudgetUiIntent.AmountChanged -> _uiState.update { state ->
                val editor = state.editor ?: return@update state
                state.copy(editor = editor.copy(
                    amountInput = intent.value,
                    error = if (editor.error == null) null else validateAmount(intent.value),
                ))
            }
            MonthlyBudgetUiIntent.CancelEditing -> _uiState.update { it.copy(editor = null) }
            MonthlyBudgetUiIntent.ApplyBudget -> applyBudget()
            MonthlyBudgetUiIntent.ViewExpenses -> {
                val month = _uiState.value.month
                viewModelScope.launch { _effects.emit(MonthlyBudgetUiEffect.NavigateToExpenses(month)) }
            }
        }
    }

    private fun applyBudget() {
        val state = _uiState.value
        val editor = state.editor ?: return
        val error = validateAmount(editor.amountInput)
        if (error != null) {
            _uiState.value = state.copy(editor = editor.copy(error = error))
            return
        }
        draftBudgets[state.month] = editor.amountInput.toLong()
        _uiState.value = stateForMonth(state.month)
    }

    private fun validateAmount(input: String): BudgetAmountInputError? = when {
        input.isBlank() -> BudgetAmountInputError.REQUIRED
        input.any { it !in '0'..'9' } -> BudgetAmountInputError.INVALID_INTEGER
        input.toLongOrNull() == null -> BudgetAmountInputError.OUT_OF_RANGE
        else -> null // 0원 예산도 유효하다. 미설정은 별도로 null로 표현한다.
    }

    private fun stateForMonth(month: YearMonth) = MonthlyBudgetUiState(
        month = month,
        // 미집계 상태를 0원 지출로 대체하지 않는다.
        summary = calculateSummary(draftBudgets[month], categoryTotals = null).toUiModel(),
        isTemporaryBudget = month in draftBudgets,
    )
}
