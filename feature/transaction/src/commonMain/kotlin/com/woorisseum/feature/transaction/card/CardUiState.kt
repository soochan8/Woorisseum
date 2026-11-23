package com.woorisseum.feature.transaction.card

import com.woorisseum.core.domain.transaction.card.RegisteredCard

data class CardListUiState(
    val cards: List<RegisteredCard> = emptyList(),
    val deletingCardId: String? = null,
    val deleteFailed: Boolean = false,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

sealed interface CardListUiIntent {
    data object Retry : CardListUiIntent
    data object AddCard : CardListUiIntent
    data class EditCard(val cardId: String) : CardListUiIntent
    data class DeleteCard(val cardId: String) : CardListUiIntent
}

sealed interface CardListUiEffect {
    data class OpenEditor(val cardId: String?) : CardListUiEffect
}

data class CardEditorUiState(
    val cardId: String? = null,
    val alias: String = "",
    val issuer: String = "",
    val aliasRequired: Boolean = false,
    val issuerRequired: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
    val loadFailed: Boolean = false,
)

sealed interface CardEditorUiIntent {
    data class AliasChanged(val value: String) : CardEditorUiIntent
    data class IssuerChanged(val value: String) : CardEditorUiIntent
    data object Retry : CardEditorUiIntent
    data object Save : CardEditorUiIntent
}
