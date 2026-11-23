package com.woorisseum.feature.transaction.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.card.CardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardEditorViewModel(private val repository: CardRepository, private val cardId: String?) : ViewModel() {
    private val _uiState = MutableStateFlow(CardEditorUiState(cardId = cardId, isLoading = cardId != null))
    val uiState = _uiState.asStateFlow()

    init { loadCard() }

    private fun loadCard() {
        if (cardId == null) return
        _uiState.update { it.copy(isLoading = true, loadFailed = false, notFound = false) }
        viewModelScope.launch {
            when (val result = repository.observeCards().first()) {
                is AppResult.Success -> {
                    val card = result.data.firstOrNull { it.id == cardId }
                    _uiState.update { it.copy(alias = card?.alias.orEmpty(), issuer = card?.issuer.orEmpty(),
                        isLoading = false, notFound = card == null) }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onIntent(intent: CardEditorUiIntent) {
        val state = _uiState.value
        if (state.isLoading || state.notFound || state.isSaving || state.saved) return
        if (intent == CardEditorUiIntent.Retry) { loadCard(); return }
        if (state.loadFailed) return
        when (intent) {
            CardEditorUiIntent.Retry -> Unit
            is CardEditorUiIntent.AliasChanged -> _uiState.update {
                it.copy(alias = intent.value, aliasRequired = it.aliasRequired && intent.value.isBlank(), saveFailed = false)
            }
            is CardEditorUiIntent.IssuerChanged -> _uiState.update {
                it.copy(issuer = intent.value, issuerRequired = it.issuerRequired && intent.value.isBlank(), saveFailed = false)
            }
            CardEditorUiIntent.Save -> save(state)
        }
    }

    private fun save(state: CardEditorUiState) {
        val alias = state.alias.trim()
        val issuer = state.issuer.trim()
        val invalid = alias.isEmpty() || issuer.isEmpty()
        _uiState.value = state.copy(aliasRequired = alias.isEmpty(), issuerRequired = issuer.isEmpty(),
            isSaving = !invalid, saveFailed = false)
        if (invalid) return
        viewModelScope.launch {
            val result = if (cardId == null) repository.addCard(alias, issuer)
                else repository.updateCard(cardId, alias, issuer)
            // 저장 성공은 State에 남겨 수집이 잠시 중단되어도 복귀 시 처리한다.
            _uiState.update { it.copy(isSaving = false, saved = result is AppResult.Success,
                saveFailed = result is AppResult.Failure) }
        }
    }
}
