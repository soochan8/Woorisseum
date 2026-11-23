package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.transaction.card.CardRepository
import com.woorisseum.feature.transaction.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.woorisseum.core.domain.common.AppResult
import kotlinx.datetime.LocalDate

/** 입력 전이를 소유한다. 저장소 연결 전에는 성공·화면 이동을 발생시키지 않는다. */
class ExpenseEntryViewModel(initialDate: LocalDate, private val cardRepository: CardRepository) : ViewModel() {
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
        if (intent is ExpenseEntryUiIntent.FormChanged && intent.intent == ExpenseFormUiIntent.RetryCards) {
            observeCards()
            return
        }
        _uiState.update { state ->
            when (intent) {
                is ExpenseEntryUiIntent.FormChanged -> state.copy(
                    form = state.form.changed(intent.intent), showStorageUnavailable = false,
                )
                ExpenseEntryUiIntent.Submit -> {
                    val form = state.form.validated()
                    state.copy(form = form, showStorageUnavailable = !form.hasErrors)
                }
            }
        }
    }
}
