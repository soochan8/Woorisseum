package com.woorisseum.core.data.budget

import androidx.sqlite.SQLiteException
import com.woorisseum.core.data.local.BudgetDao
import com.woorisseum.core.data.local.BudgetEntity
import com.woorisseum.core.domain.budget.repository.BudgetRepository
import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

internal class LocalBudgetRepository(private val dao: BudgetDao) : BudgetRepository {
    override fun observeBudget(month: YearMonth): Flow<AppResult<Long?>> =
        dao.observeAmount(month.year, month.month.number)
            .map<Long?, AppResult<Long?>> { AppResult.Success(it) }
            .distinctUntilChanged()
            .catch { error ->
                if (error is SQLiteException) emit(AppResult.Failure(AppError.Storage)) else throw error
            }

    override suspend fun saveBudget(month: YearMonth, amount: Long): AppResult<Unit> {
        if (amount < 0L) return AppResult.Failure(AppError.InvalidInput)
        return try {
            dao.save(BudgetEntity(month.year, month.month.number, amount))
            AppResult.Success(Unit)
        } catch (_: SQLiteException) {
            AppResult.Failure(AppError.Storage)
        }
    }
}
