package com.woorisseum.core.domain.transaction.model

/** 지출에 사용한 결제수단이다. 신용·체크카드는 카드명을 선택하는 동일한 방식으로 다룬다. */
sealed interface PaymentMethod {
    data object Cash : PaymentMethod

    data object BankTransfer : PaymentMethod

    /** 동명이거나 이름이 바뀐 카드를 구분할 수 있도록 식별자와 이름을 함께 전달한다. */
    data class Card(
        val cardId: String,
        val name: String,
    ) : PaymentMethod
}
