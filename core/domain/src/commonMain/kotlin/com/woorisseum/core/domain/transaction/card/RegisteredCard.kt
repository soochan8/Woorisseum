package com.woorisseum.core.domain.transaction.card

/** 결제 연동 정보 없이 별칭과 카드사만 관리한다. 동명 카드도 ID로 구분한다. */
data class RegisteredCard(val id: String, val alias: String, val issuer: String)
