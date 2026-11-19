package com.woorisseum.core.data.transaction.card

import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.card.CardRepository
import com.woorisseum.core.domain.transaction.card.RegisteredCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 앱 실행 중에만 공유하는 카드 목록이다. 초기 예시 데이터는 넣지 않는다. */
class InMemoryCardRepository : CardRepository {
    private val cards = MutableStateFlow<List<RegisteredCard>>(emptyList())
    private val mutex = Mutex()
    private var nextId = 1L

    override fun observeCards() = cards.asStateFlow()

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
