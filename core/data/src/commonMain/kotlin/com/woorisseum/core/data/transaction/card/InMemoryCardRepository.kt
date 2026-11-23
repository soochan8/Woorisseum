package com.woorisseum.core.data.transaction.card

import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.card.CardRepository
import com.woorisseum.core.domain.transaction.card.RegisteredCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** IDE Preview 전용 카드 목록이다. 운영 앱은 LocalCardRepository를 사용한다. */
internal class InMemoryCardRepository : CardRepository {
    private val cards = MutableStateFlow<List<RegisteredCard>>(emptyList())
    private val mutex = Mutex()
    private var nextId = 1L

    override fun observeCards() = cards.map { AppResult.Success(it) }

    override suspend fun addCard(alias: String, issuer: String): AppResult<RegisteredCard> = mutex.withLock {
        require(alias.isNotBlank() && alias == alias.trim())
        require(issuer.isNotBlank() && issuer == issuer.trim())
        val card = RegisteredCard("card-${nextId++}", alias, issuer)
        cards.value = cards.value + card
        AppResult.Success(card)
    }

    override suspend fun updateCard(id: String, alias: String, issuer: String): AppResult<RegisteredCard> = mutex.withLock {
        require(alias.isNotBlank() && alias == alias.trim())
        require(issuer.isNotBlank() && issuer == issuer.trim())
        if (cards.value.none { it.id == id }) return@withLock AppResult.Failure(AppError.Storage)
        val card = RegisteredCard(id, alias, issuer)
        cards.value = cards.value.map { if (it.id == id) card else it }
        AppResult.Success(card)
    }
    override suspend fun deleteCard(id: String): AppResult<Unit> = mutex.withLock {
        cards.value = cards.value.filterNot { it.id == id }
        AppResult.Success(Unit)
    }

}
