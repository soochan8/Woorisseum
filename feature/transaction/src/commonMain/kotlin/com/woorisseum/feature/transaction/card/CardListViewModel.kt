package com.woorisseum.feature.transaction.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.transaction.card.CardRepository
import com.woorisseum.core.domain.common.AppResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardListViewModel(private val repository: CardRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CardListUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<CardListUiEffect>()
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch { repository.observeCards().collect { cards -> _uiState.update { it.copy(cards = cards) } } }
    }

    fun onIntent(intent: CardListUiIntent) {
        if (_uiState.value.deletingCardId != null) return
        when (intent) {
            CardListUiIntent.AddCard -> openEditor(null)
            is CardListUiIntent.EditCard -> openEditor(intent.cardId)
            is CardListUiIntent.DeleteCard -> {
                if (_uiState.value.cards.none { it.id == intent.cardId }) return
                _uiState.update { it.copy(deletingCardId = intent.cardId, deleteFailed = false) }
                viewModelScope.launch {
                    val result = repository.deleteCard(intent.cardId)
                    _uiState.update { it.copy(deletingCardId = null, deleteFailed = result is AppResult.Failure) }
                }
            }
        }
    }

    private fun openEditor(cardId: String?) {
        viewModelScope.launch { _effects.emit(CardListUiEffect.OpenEditor(cardId)) }
    }
}
