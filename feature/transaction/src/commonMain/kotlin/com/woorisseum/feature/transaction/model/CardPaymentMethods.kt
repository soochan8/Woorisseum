package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.card.RegisteredCard
import com.woorisseum.core.domain.transaction.model.PaymentMethod

internal fun List<RegisteredCard>.toPaymentMethods(): List<PaymentMethod> =
    listOf(PaymentMethod.Cash, PaymentMethod.BankTransfer) + map { PaymentMethod.Card(it.id, it.alias, it.issuer) }

/** 삭제된 등록 카드는 제거하되, 수정 중인 지출의 원래 카드만 선택지에 보존한다. */
internal fun ExpenseFormUiState.withCards(
    cards: List<RegisteredCard>,
    originalCard: PaymentMethod.Card? = null,
): ExpenseFormUiState {
    val methods = cards.toPaymentMethods()
    val available = if (originalCard != null && cards.none { it.id == originalCard.cardId }) {
        methods + originalCard
    } else methods
    val selectedCard = paymentMethod as? PaymentMethod.Card
    val selected = if (selectedCard == null) paymentMethod else
        available.filterIsInstance<PaymentMethod.Card>().firstOrNull { it.cardId == selectedCard.cardId }
    return copy(availablePaymentMethods = available, paymentMethod = selected, cardLoadFailed = false)
}

/** 선택지나 카드 표시명 변경만으로 지출 수정 여부가 바뀌지 않게 한다. */
internal fun ExpenseFormUiState.inputValues(): ExpenseFormUiState = copy(
    availablePaymentMethods = emptyList(), amountError = null, paymentMethodRequired = false, cardLoadFailed = false,
    paymentMethod = (paymentMethod as? PaymentMethod.Card)?.copy(name = "", issuer = "") ?: paymentMethod,
)
