package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.transaction.card.CardRepository
import com.woorisseum.core.domain.transaction.repository.ExpenseRepository
import com.woorisseum.feature.transaction.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.woorisseum.core.domain.common.AppResult
import kotlinx.datetime.LocalDate

/** 입력 검증과 저장 상태를 소유하며 DB 반영이 끝난 뒤 성공을 남긴다. */
class ExpenseEntryViewModel(initialDate: LocalDate, private val cardRepository: CardRepository, private val repository: ExpenseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseEntryUiState(ExpenseFormUiState(date = initialDate)))
    val uiState = _uiState.asStateFlow()

    private var cardObservation: Job? = null

    init { observeCards() }

    private fun observeCards() {
        cardObservation?.cancel()
        cardObservation = viewModelScope.launch {
            cardRepository.observeCards().collect { result ->
                if (result is AppResult.Failure) {
                    _uiState.update { state -> state.copy(form = state.form.copy(cardLoadFailed = true)) }
                    return@collect
                }
                val cards = (result as AppResult.Success).data
                _uiState.update { it.copy(form = it.form.withCards(cards)) }
            }
        }
    }

    fun onIntent(intent: ExpenseEntryUiIntent) {
        val state = _uiState.value
        if (state.isSaving || state.saved) return
        when (intent) {
            is ExpenseEntryUiIntent.FormChanged -> {
                if (intent.intent == ExpenseFormUiIntent.RetryCards) observeCards()
                else _uiState.update { it.copy(form = it.form.changed(intent.intent), saveError = null) }
            }
            ExpenseEntryUiIntent.Submit -> {
                val form = state.form.validated()
                _uiState.value = state.copy(form = form, isSaving = !form.hasErrors, saveError = null)
                if (form.hasErrors) return
                val input = form.toNewExpense()
                viewModelScope.launch {
                    when (val result = repository.addExpense(input)) {
                        is AppResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                        is AppResult.Failure -> _uiState.update { it.copy(isSaving = false,
                            saveError = result.error.toExpenseOperationError()) }
                    }
                }
            }
        }
    }
}
