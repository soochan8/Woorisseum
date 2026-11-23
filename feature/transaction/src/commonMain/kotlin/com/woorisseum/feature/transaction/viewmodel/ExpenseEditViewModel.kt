package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.card.CardRepository
import com.woorisseum.core.domain.transaction.card.RegisteredCard
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.core.domain.transaction.repository.ExpenseRepository
import com.woorisseum.feature.transaction.mapper.toFormUiState
import com.woorisseum.feature.transaction.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** 조회 원본의 버전으로 수정한다. 충돌·실패 시 초안을 자동으로 덮어쓰지 않는다. */
class ExpenseEditViewModel(
    private val expenseId: String, private val cardRepository: CardRepository,
    private val repository: ExpenseRepository,
) : ViewModel() {
    private var original: Expense? = null
    private var cards: List<RegisteredCard> = emptyList()
    private var cardsFailed = false
    private val _uiState = MutableStateFlow(ExpenseEditUiState(expenseId, isLoading = true))
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<ExpenseEditUiEffect>()
    val effects = _effects.asSharedFlow()
    private var cardObservation: Job? = null
    private var loading: Job? = null

    init { observeCards(); loadExpense() }

    private fun originalForm() = original?.toFormUiState()
    private fun presentForm(form: ExpenseFormUiState) = form.withCards(cards, original?.paymentMethod as? PaymentMethod.Card)
        .copy(cardLoadFailed = cardsFailed)

    private fun observeCards() {
        cardObservation?.cancel()
        cardObservation = viewModelScope.launch {
            cardRepository.observeCards().collect { result ->
                cardsFailed = result is AppResult.Failure
                if (result is AppResult.Success) cards = result.data
                _uiState.update { state ->
                    val form = state.form?.let(::presentForm)
                    state.copy(form = form, hasChanges = form != null && form.inputValues() != originalForm()?.inputValues())
                }
            }
        }
    }

    private fun loadExpense() {
        loading?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loading = viewModelScope.launch {
            when (val result = repository.getExpense(expenseId)) {
                is AppResult.Success -> {
                    original = result.data
                    _uiState.value = ExpenseEditUiState(expenseId, presentForm(result.data.toFormUiState()))
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false,
                    loadError = result.error.toExpenseOperationError()) }
            }
        }
    }

    fun onIntent(intent: ExpenseEditUiIntent) {
        val state = _uiState.value
        if (state.isSaving || state.saved) return
        when (intent) {
            ExpenseEditUiIntent.Reload -> if (!state.isLoading) loadExpense()
            ExpenseEditUiIntent.Cancel -> viewModelScope.launch { _effects.emit(ExpenseEditUiEffect.NavigateBack) }
            is ExpenseEditUiIntent.FormChanged -> {
                if (state.isLoading) return
                if (intent.intent == ExpenseFormUiIntent.RetryCards) { observeCards(); return }
                _uiState.update {
                    val form = it.form?.changed(intent.intent) ?: return@update it
                    it.copy(form = form, hasChanges = form.inputValues() != originalForm()?.inputValues(),
                        saveError = it.saveError?.takeIf { error -> error == ExpenseOperationError.CONFLICT || error == ExpenseOperationError.NOT_FOUND })
                }
            }
            ExpenseEditUiIntent.Submit -> {
                if (state.isLoading || !state.hasChanges || state.saveError == ExpenseOperationError.CONFLICT ||
                    state.saveError == ExpenseOperationError.NOT_FOUND) return
                val form = state.form?.validated() ?: return
                val version = original?.revision ?: return
                _uiState.value = state.copy(form = form, isSaving = !form.hasErrors, saveError = null)
                if (form.hasErrors) return
                val input = form.toNewExpense()
                viewModelScope.launch {
                    when (val result = repository.updateExpense(expenseId, input, version)) {
                        is AppResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                        is AppResult.Failure -> _uiState.update { it.copy(isSaving = false,
                            saveError = result.error.toExpenseOperationError()) }
                    }
                }
            }
        }
    }
}
