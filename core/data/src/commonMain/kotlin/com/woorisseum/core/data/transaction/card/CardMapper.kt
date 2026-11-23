package com.woorisseum.core.data.transaction.card

import com.woorisseum.core.data.local.CardEntity
import com.woorisseum.core.domain.transaction.card.RegisteredCard

internal fun CardEntity.toDomain() = RegisteredCard(id = id, alias = alias, issuer = issuer)

internal fun RegisteredCard.toEntity() = CardEntity(id = id, alias = alias, issuer = issuer)
