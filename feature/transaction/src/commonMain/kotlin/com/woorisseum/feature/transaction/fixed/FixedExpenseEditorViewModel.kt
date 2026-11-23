package com.woorisseum.feature.transaction.fixed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.fixed.*
import com.woorisseum.feature.transaction.model.ExpenseOperationError
import com.woorisseum.feature.transaction.model.toExpenseOperationError
import com.woorisseum.feature.transaction.model.validateExpenseAmount
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth

/** 원본 버전과 입력을 분리하며 저장·중단 성공은 State에 보존한다. */
class FixedExpenseEditorViewModel(
    private val initialMonth: YearMonth,
    private val planId: String? = null,
    private val repository: FixedExpenseRepository,
) : ViewModel() {
    private var original: FixedExpensePlan? = null
    private var originalForm: FixedExpenseFormUiState? = null
    private var loading: Job? = null
    private val _uiState = MutableStateFlow(FixedExpenseEditorUiState(planId,
        if (planId == null) FixedExpenseFormUiState(initialMonth) else null, isLoading = planId != null))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<FixedExpenseEditorUiEffect>()
    val effects = _effects.asSharedFlow()

    init { if (planId != null) loadPlan() }

    private fun loadPlan() {
        val id = planId ?: return
        loading?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loading = viewModelScope.launch {
            when (val result = repository.getPlan(id, initialMonth)) {
                is AppResult.Success -> {
                    original = result.data
                    originalForm = result.data.toFormUiState().copy(startMonth = initialMonth)
                    _uiState.value = FixedExpenseEditorUiState(planId, originalForm)
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, loadError = result.error.toExpenseOperationError()) }
            }
        }
    }

    fun onIntent(intent: FixedExpenseEditorUiIntent) {
        val state = _uiState.value
        if (state.isSaving || state.saved) return
        if (intent == FixedExpenseEditorUiIntent.Cancel) {
            viewModelScope.launch { _effects.emit(FixedExpenseEditorUiEffect.NavigateBack) }; return
        }
        if (intent == FixedExpenseEditorUiIntent.Reload) { loadPlan(); return }
        if (state.isLoading || state.loadError != null) return
        val form = state.form ?: return
        if (intent == FixedExpenseEditorUiIntent.Stop || intent == FixedExpenseEditorUiIntent.Submit) {
            if (state.saveError == ExpenseOperationError.CONFLICT || state.saveError == ExpenseOperationError.NOT_FOUND) return
            if (intent == FixedExpenseEditorUiIntent.Submit && planId != null && !state.hasChanges) return
            val checked = form.validated()
            if (intent == FixedExpenseEditorUiIntent.Submit && checked.hasErrors()) {
                _uiState.update { it.copy(form = checked) }; return
            }
            val revision = original?.revision
            if (intent == FixedExpenseEditorUiIntent.Stop && (planId == null || revision == null)) return
            _uiState.update { it.copy(form = if (intent == FixedExpenseEditorUiIntent.Submit) checked else form,
                isSaving = true, saveError = null) }
            viewModelScope.launch {
                val result = when {
                    intent == FixedExpenseEditorUiIntent.Stop -> repository.stopPlan(requireNotNull(planId), form.startMonth, requireNotNull(revision))
                    planId == null -> repository.addPlan(form.startMonth, checked.toInput())
                    else -> repository.updatePlan(planId, form.startMonth, checked.toInput(), requireNotNull(revision))
                }
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                    is AppResult.Failure -> _uiState.update { it.copy(isSaving = false, saveError = result.error.toExpenseOperationError()) }
                }
            }
            return
        }
        val changed = when (intent) {
            is FixedExpenseEditorUiIntent.TypeSelected -> form.copy(type = intent.type)
            is FixedExpenseEditorUiIntent.NameChanged -> form.copy(nameInput = intent.value)
            is FixedExpenseEditorUiIntent.AmountChanged -> form.copy(amountInput = intent.value)
            is FixedExpenseEditorUiIntent.BillingDayChanged -> form.copy(billingDayInput = intent.value)
            is FixedExpenseEditorUiIntent.MemoChanged -> form.copy(memoInput = intent.value)
            FixedExpenseEditorUiIntent.PreviousStartMonth -> form.copy(startMonth = form.startMonth.minusMonth())
            FixedExpenseEditorUiIntent.NextStartMonth -> form.copy(startMonth = form.startMonth.plusMonth())
        }
        _uiState.update { it.copy(form = if (form.hasErrors()) changed.validated() else changed,
            hasChanges = changed.inputValues() != originalForm?.inputValues(),
            saveError = it.saveError?.takeIf { error -> error == ExpenseOperationError.CONFLICT || error == ExpenseOperationError.NOT_FOUND }) }
    }
}
private fun FixedExpenseFormUiState.hasErrors() = typeRequired || nameRequired || amountError != null || invalidBillingDay
private fun FixedExpenseFormUiState.inputValues() = copy(nameInput = nameInput.trim(), memoInput = memoInput.trim(),
    typeRequired = false, nameRequired = false, amountError = null, invalidBillingDay = false)
private fun FixedExpenseFormUiState.validated() = copy(
    typeRequired = type == null,
    nameRequired = type == FixedExpenseType.OTHER && nameInput.isBlank(),
    amountError = validateExpenseAmount(amountInput),
    invalidBillingDay = billingDayInput.isEmpty() || billingDayInput.any { it !in '0'..'9' } ||
        billingDayInput.toIntOrNull()?.let { it in 1..31 } != true,
)
