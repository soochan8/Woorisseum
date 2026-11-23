package com.woorisseum.core.data.transaction.card

import androidx.sqlite.SQLiteException
import com.woorisseum.core.data.local.CardDao
import com.woorisseum.core.data.local.CardEntity
import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.card.CardRepository
import com.woorisseum.core.domain.transaction.card.RegisteredCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

/** DB를 카드 목록의 원본으로 사용하고 저장 확정 후에만 성공을 반환한다. */
internal class LocalCardRepository(private val dao: CardDao) : CardRepository {
    override fun observeCards(): Flow<AppResult<List<RegisteredCard>>> = dao.observeCards()
        .map<List<CardEntity>, AppResult<List<RegisteredCard>>> { rows ->
            AppResult.Success(rows.map { it.toDomain() })
        }
        .distinctUntilChanged()
        .catch { error ->
            if (error is SQLiteException) emit(AppResult.Failure(AppError.Storage)) else throw error
        }

    override suspend fun addCard(alias: String, issuer: String): AppResult<RegisteredCard> {
        validate(alias, issuer)
        val card = RegisteredCard(Uuid.random().toString(), alias, issuer)
        return try {
            dao.insert(card.toEntity())
            AppResult.Success(card)
        } catch (_: SQLiteException) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun updateCard(id: String, alias: String, issuer: String): AppResult<RegisteredCard> {
        validate(alias, issuer)
        return try {
            if (dao.update(id, alias, issuer) == 1) AppResult.Success(RegisteredCard(id, alias, issuer))
            else AppResult.Failure(AppError.Storage)
        } catch (_: SQLiteException) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun deleteCard(id: String): AppResult<Unit> = try {
        dao.delete(id)
        AppResult.Success(Unit)
    } catch (_: SQLiteException) {
        AppResult.Failure(AppError.Storage)
    }

    private fun validate(alias: String, issuer: String) {
        require(alias.isNotBlank() && alias == alias.trim())
        require(issuer.isNotBlank() && issuer == issuer.trim())
    }
}
