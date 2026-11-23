package com.woorisseum.core.data.transaction

import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteException
import com.woorisseum.core.data.local.ExpenseEntity
import com.woorisseum.core.data.local.WoorisseumDatabase
import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.core.domain.transaction.model.NewExpense
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.core.domain.transaction.model.isValidExpenseAmount
import com.woorisseum.core.domain.transaction.repository.ExpenseRepository
import com.woorisseum.core.domain.transaction.search.ExpenseSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth
import kotlin.uuid.Uuid

/** 카드 확인과 지출 저장을 같은 쓰기 트랜잭션에서 확정한다. */
internal class LocalExpenseRepository(private val database: WoorisseumDatabase) : ExpenseRepository {
    private val dao = database.expenseDao()

    override suspend fun addExpense(input: NewExpense): AppResult<Expense> {
        if (!input.isValid()) return AppResult.Failure(AppError.InvalidInput)
        return storageResult {
            database.withWriteTransaction {
                val payment = resolvePayment(input.paymentMethod)
                    ?: return@withWriteTransaction AppResult.Failure(AppError.InvalidInput)
                val expense = input.toExpense(Uuid.random().toString(), payment, revision = 0)
                dao.insert(expense.toEntity())
                AppResult.Success(expense)
            }
        }
    }

    override suspend fun getExpense(id: String): AppResult<Expense> = storageResult {
        dao.findById(id)?.let { AppResult.Success(it.toDomain()) }
            ?: AppResult.Failure(AppError.NotFound)
    }

    override suspend fun updateExpense(id: String, input: NewExpense, expectedRevision: Long): AppResult<Expense> {
        if (!input.isValid() || expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            return AppResult.Failure(AppError.InvalidInput)
        }
        return storageResult {
            database.withWriteTransaction {
                val original = dao.findById(id)?.toDomain()
                    ?: return@withWriteTransaction AppResult.Failure(AppError.NotFound)
                if (original.revision != expectedRevision) {
                    return@withWriteTransaction AppResult.Failure(AppError.Conflict)
                }
                val payment = resolvePayment(input.paymentMethod, original.paymentMethod)
                    ?: return@withWriteTransaction AppResult.Failure(AppError.InvalidInput)
                val expense = input.toExpense(id, payment, expectedRevision + 1)
                val row = expense.toEntity()
                val changed = dao.update(row.id, expectedRevision, row.amount, row.dateEpochDay,
                    row.merchant, row.category, row.paymentType, row.cardId, row.cardName, row.cardIssuer, row.memo)
                if (changed == 1) AppResult.Success(expense) else AppResult.Failure(AppError.Conflict)
            }
        }
    }

    override fun observeExpenses(month: YearMonth): Flow<AppResult<List<Expense>>> =
        observeExpenses(ExpenseSearch(month = month))

    override fun observeExpenses(search: ExpenseSearch): Flow<AppResult<List<Expense>>> {
        val start = search.start
        val end = search.endInclusive
        if (start != null && end != null && start > end) {
            return flowOf(AppResult.Failure(AppError.InvalidInput))
        }
        // 날짜 객체에 다음 날을 더하지 않아 표현 가능한 마지막 날도 안전하게 포함한다.
        val lower = maxOf(start?.toEpochDays() ?: Long.MIN_VALUE,
            search.month?.firstDay?.toEpochDays() ?: Long.MIN_VALUE)
        val upper = minOf(end?.toEpochDays()?.plus(1) ?: Long.MAX_VALUE,
            search.month?.lastDay?.toEpochDays()?.plus(1) ?: Long.MAX_VALUE)
        if (lower >= upper) return flowOf(AppResult.Success(emptyList()))
        return dao.observeExpenses(lower, upper)
            .map<List<ExpenseEntity>, AppResult<List<Expense>>> { rows ->
                // Unicode·검색 기호 처리를 SQLite LIKE 대신 기존 domain 계약과 일치시킨다.
                AppResult.Success(rows.map { it.toDomain() }.filter(search::matches))
            }
            .distinctUntilChanged()
            .catch { error ->
                if (error is SQLiteException || error is InvalidExpenseRecord) {
                    emit(AppResult.Failure(AppError.Storage))
                } else throw error
            }
    }

    /** 같은 카드는 당시 정보를 유지하고, 다른 카드는 현재 등록 정보에서 선택한다. */
    private suspend fun resolvePayment(requested: PaymentMethod, original: PaymentMethod? = null): PaymentMethod? {
        if (requested !is PaymentMethod.Card) return requested
        if (original is PaymentMethod.Card && requested.cardId == original.cardId) return original
        val card = database.cardDao().findById(requested.cardId) ?: return null
        return PaymentMethod.Card(card.id, card.alias, card.issuer)
    }
}

private fun NewExpense.isValid(): Boolean {
    val payment = paymentMethod
    return isValidExpenseAmount(amount) && (payment !is PaymentMethod.Card || payment.cardId.isNotBlank())
}

private fun NewExpense.toExpense(id: String, payment: PaymentMethod, revision: Long) = Expense(
    id = id, amount = amount, date = date,
    merchant = merchant?.trim()?.takeIf { it.isNotEmpty() }, category = category,
    paymentMethod = payment, memo = memo?.trim()?.takeIf { it.isNotEmpty() }, revision = revision,
)

/** 예상 가능한 DB 오류만 변환하며 취소·프로그래밍 오류는 전파한다. */
private suspend fun <T> storageResult(block: suspend () -> AppResult<T>): AppResult<T> = try {
    block()
} catch (_: SQLiteException) {
    AppResult.Failure(AppError.Storage)
} catch (_: InvalidExpenseRecord) {
    AppResult.Failure(AppError.Storage)
}
