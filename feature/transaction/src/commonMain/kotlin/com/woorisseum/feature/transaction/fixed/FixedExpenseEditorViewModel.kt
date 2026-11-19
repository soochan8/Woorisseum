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

/** 등록·수정 입력을 관리한다. 제출해도 실제 저장이나 납부·거래 생성은 수행하지 않는다. */
class FixedExpenseEditorViewModel(
    initialMonth: YearMonth,
    planId: String? = null,
    initialPlan: FixedExpensePlan? = null,
) : ViewModel() {
    init { require(initialPlan == null || initialPlan.id == planId) }
    private val original = initialPlan?.toFormUiState()
    private val _uiState = MutableStateFlow(FixedExpenseEditorUiState(planId,
        if (planId == null) FixedExpenseFormUiState(initialMonth) else original))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<FixedExpenseEditorUiEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: FixedExpenseEditorUiIntent) {
        if (intent == FixedExpenseEditorUiIntent.Cancel) {
            viewModelScope.launch { _effects.emit(FixedExpenseEditorUiEffect.NavigateBack) }
            return
        }
        _uiState.update { state ->
            val form = state.form ?: return@update state
            if (intent == FixedExpenseEditorUiIntent.Submit) {
                if (state.planId != null && !state.hasChanges) return@update state
                val validated = form.validated()
                return@update state.copy(form = validated, showStorageUnavailable = !validated.hasErrors())
            }
            val changed = when (intent) {
                is FixedExpenseEditorUiIntent.TypeSelected -> form.copy(type = intent.type)
                is FixedExpenseEditorUiIntent.NameChanged -> form.copy(nameInput = intent.value)
                is FixedExpenseEditorUiIntent.AmountChanged -> form.copy(amountInput = intent.value)
                is FixedExpenseEditorUiIntent.BillingDayChanged -> form.copy(billingDayInput = intent.value)
                is FixedExpenseEditorUiIntent.MemoChanged -> form.copy(memoInput = intent.value)
                FixedExpenseEditorUiIntent.PreviousStartMonth -> form.copy(startMonth = form.startMonth.minusMonth())
                FixedExpenseEditorUiIntent.NextStartMonth -> form.copy(startMonth = form.startMonth.plusMonth())
                FixedExpenseEditorUiIntent.Cancel, FixedExpenseEditorUiIntent.Submit -> form
            }
            state.copy(form = if (form.hasErrors()) changed.validated() else changed,
                hasChanges = changed.withoutErrors() != original, showStorageUnavailable = false)
        }
    }
}
private fun FixedExpenseFormUiState.hasErrors() = typeRequired || nameRequired || amountError != null || invalidBillingDay
private fun FixedExpenseFormUiState.withoutErrors() = copy(typeRequired = false, nameRequired = false, amountError = null, invalidBillingDay = false)
private fun FixedExpenseFormUiState.validated() = copy(
    typeRequired = type == null,
    nameRequired = type == FixedExpenseType.OTHER && nameInput.isBlank(),
    amountError = validateExpenseAmount(amountInput),
    invalidBillingDay = billingDayInput.isEmpty() || billingDayInput.any { it !in '0'..'9' } ||
        billingDayInput.toIntOrNull()?.let { it in 1..31 } != true,
)
