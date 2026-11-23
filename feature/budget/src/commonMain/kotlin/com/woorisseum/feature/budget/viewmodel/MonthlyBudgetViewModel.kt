package com.woorisseum.feature.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.budget.repository.BudgetRepository
import com.woorisseum.core.domain.budget.usecase.CalculateBudgetSummaryUseCase
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.repository.ExpenseRepository
import com.woorisseum.feature.budget.mapper.toUiModel
import com.woorisseum.feature.budget.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth

/** 실제 월 예산과 지출을 관찰하며 입력 초안은 조회 갱신과 독립적으로 유지한다. */
class MonthlyBudgetViewModel(
    initialMonth: YearMonth,
    private val calculateSummary: CalculateBudgetSummaryUseCase,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {
    private var budgetAmount: Long? = null
    private val _uiState = MutableStateFlow(stateForMonth(initialMonth))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<MonthlyBudgetUiEffect>()
    val effects = _effects.asSharedFlow()
    private var observation: Job? = null

    init { observeMonth(initialMonth) }

    private fun observeMonth(month: YearMonth) {
        observation?.cancel()
        budgetAmount = null
        _uiState.value = stateForMonth(month)
        observation = viewModelScope.launch {
            combine(budgetRepository.observeBudget(month), expenseRepository.observeExpenses(month)) { budget, expenses ->
                budget to expenses
            }.collect { (budget, expenses) ->
                if (_uiState.value.month != month) return@collect
                val failed = budget is AppResult.Failure || expenses is AppResult.Failure
                if (budget is AppResult.Success) budgetAmount = budget.data
                val summary = if (budget is AppResult.Success && expenses is AppResult.Success) {
                    calculateSummary.fromExpenses(budget.data, expenses.data)
                } else calculateSummary(budgetAmount, null)
                _uiState.update { it.copy(summary = summary.toUiModel(), isLoading = false, loadFailed = failed) }
            }
        }
    }

    fun onIntent(intent: MonthlyBudgetUiIntent) {
        val state = _uiState.value
        if (state.editor?.isSaving == true) return
        when (intent) {
            MonthlyBudgetUiIntent.Retry -> observeMonth(state.month)
            MonthlyBudgetUiIntent.PreviousMonth -> observeMonth(state.month.minusMonth())
            MonthlyBudgetUiIntent.NextMonth -> observeMonth(state.month.plusMonth())
            MonthlyBudgetUiIntent.EditBudget -> if (!state.isLoading && !state.loadFailed) {
                _uiState.update { it.copy(editor = BudgetEditorUiState(budgetAmount?.toString().orEmpty())) }
            }
            is MonthlyBudgetUiIntent.AmountChanged -> _uiState.update {
                val editor = it.editor ?: return@update it
                it.copy(editor = editor.copy(amountInput = intent.value, saveFailed = false,
                    error = if (editor.error == null) null else validateAmount(intent.value)))
            }
            MonthlyBudgetUiIntent.CancelEditing -> _uiState.update { it.copy(editor = null) }
            MonthlyBudgetUiIntent.ApplyBudget -> applyBudget()
            MonthlyBudgetUiIntent.ViewExpenses -> viewModelScope.launch {
                _effects.emit(MonthlyBudgetUiEffect.NavigateToExpenses(state.month))
            }
        }
    }

    private fun applyBudget() {
        val state = _uiState.value
        val editor = state.editor ?: return
        val error = validateAmount(editor.amountInput)
        _uiState.update { it.copy(editor = editor.copy(error = error, isSaving = error == null, saveFailed = false)) }
        if (error != null) return
        val amount = editor.amountInput.toLong()
        viewModelScope.launch {
            when (budgetRepository.saveBudget(state.month, amount)) {
                is AppResult.Success -> _uiState.update { it.copy(editor = null) }
                is AppResult.Failure -> _uiState.update {
                    it.copy(editor = it.editor?.copy(isSaving = false, saveFailed = true))
                }
            }
        }
    }

    private fun validateAmount(input: String): BudgetAmountInputError? = when {
        input.isBlank() -> BudgetAmountInputError.REQUIRED
        input.any { it !in '0'..'9' } -> BudgetAmountInputError.INVALID_INTEGER
        input.toLongOrNull() == null -> BudgetAmountInputError.OUT_OF_RANGE
        else -> null
    }

    private fun stateForMonth(month: YearMonth) = MonthlyBudgetUiState(
        month, calculateSummary(null, null).toUiModel(), isLoading = true,
    )
}
