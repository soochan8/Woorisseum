package com.woorisseum.core.data.transaction.fixed

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteException
import com.woorisseum.core.data.local.FixedExpenseEntity
import com.woorisseum.core.data.local.FixedExpenseSnapshot
import com.woorisseum.core.data.local.WoorisseumDatabase
import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.fixed.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth
import kotlin.uuid.Uuid

/** 계획 ID·월별 변경 이력·중단 시점을 하나의 트랜잭션으로 관리한다. */
internal class LocalFixedExpenseRepository(private val database: WoorisseumDatabase) : FixedExpenseRepository {
    private val dao = database.fixedExpenseDao()

    override fun observePlans(month: YearMonth): Flow<AppResult<List<FixedExpensePlan>>> =
        dao.observePlans(month.toStorageMonth())
            .map<List<FixedExpenseSnapshot>, AppResult<List<FixedExpensePlan>>> { rows ->
                AppResult.Success(rows.map { it.toDomain() })
            }
            .distinctUntilChanged()
            .catch { error ->
                if (error is SQLiteException || error is InvalidFixedExpenseRecord) emit(AppResult.Failure(AppError.Storage))
                else throw error
            }

    override suspend fun getPlan(id: String, month: YearMonth): AppResult<FixedExpensePlan> = storageResult {
        database.withReadTransaction {
            val plan = dao.findPlan(id)
            val value = month.toStorageMonth()
            val version = if (plan != null && (plan.stoppedFromMonth?.let { value < it } != false)) {
                dao.findVersion(id, value)
            } else null
            if (plan != null && version != null) AppResult.Success(version.toDomain(plan))
            else AppResult.Failure(AppError.NotFound)
        }
    }

    override suspend fun addPlan(startMonth: YearMonth, input: FixedExpenseInput): AppResult<FixedExpensePlan> {
        if (!input.isValid()) return AppResult.Failure(AppError.InvalidInput)
        return storageResult {
            database.withWriteTransaction {
                val plan = FixedExpenseEntity(Uuid.random().toString(), revision = 0, stoppedFromMonth = null)
                val version = input.toVersion(plan.id, startMonth)
                dao.insertPlan(plan)
                dao.insertVersion(version)
                AppResult.Success(version.toDomain(plan))
            }
        }
    }

    override suspend fun updatePlan(id: String, fromMonth: YearMonth, input: FixedExpenseInput,
        expectedRevision: Long): AppResult<FixedExpensePlan> {
        if (!input.isValid() || !validRevision(expectedRevision)) return AppResult.Failure(AppError.InvalidInput)
        return storageResult {
            database.withWriteTransaction {
                val plan = dao.findPlan(id) ?: return@withWriteTransaction AppResult.Failure(AppError.NotFound)
                if (plan.revision != expectedRevision) return@withWriteTransaction AppResult.Failure(AppError.Conflict)
                val month = fromMonth.toStorageMonth()
                if (plan.stoppedFromMonth?.let { month >= it } == true) {
                    return@withWriteTransaction AppResult.Failure(AppError.NotFound)
                }
                if (dao.findVersion(id, month) == null) return@withWriteTransaction AppResult.Failure(AppError.InvalidInput)
                if (dao.advanceRevision(id, expectedRevision) != 1) return@withWriteTransaction AppResult.Failure(AppError.Conflict)
                val version = input.toVersion(id, fromMonth)
                // 선택 월의 값만 저장한다. 이후에 예약된 다른 월의 변경은 보존한다.
                dao.saveVersion(version)
                AppResult.Success(version.toDomain(plan.copy(revision = expectedRevision + 1)))
            }
        }
    }

    override suspend fun stopPlan(id: String, fromMonth: YearMonth, expectedRevision: Long): AppResult<Unit> {
        if (!validRevision(expectedRevision)) return AppResult.Failure(AppError.InvalidInput)
        return storageResult {
            database.withWriteTransaction {
                val plan = dao.findPlan(id) ?: return@withWriteTransaction AppResult.Failure(AppError.NotFound)
                if (plan.revision != expectedRevision) return@withWriteTransaction AppResult.Failure(AppError.Conflict)
                val month = fromMonth.toStorageMonth()
                if (plan.stoppedFromMonth?.let { month >= it } == true) {
                    return@withWriteTransaction AppResult.Failure(AppError.NotFound)
                }
                if (dao.findVersion(id, month) == null) return@withWriteTransaction AppResult.Failure(AppError.InvalidInput)
                if (dao.stop(id, month, expectedRevision) == 1) AppResult.Success(Unit)
                else AppResult.Failure(AppError.Conflict)
            }
        }
    }
}

private fun validRevision(revision: Long) = revision >= 0 && revision < Long.MAX_VALUE
private suspend fun <T> storageResult(block: suspend () -> AppResult<T>): AppResult<T> = try {
    block()
} catch (_: SQLiteException) {
    AppResult.Failure(AppError.Storage)
} catch (_: InvalidFixedExpenseRecord) {
    AppResult.Failure(AppError.Storage)
}
