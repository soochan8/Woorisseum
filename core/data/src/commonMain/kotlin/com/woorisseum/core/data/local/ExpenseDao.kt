package com.woorisseum.core.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Insert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun findById(id: String): ExpenseEntity?

    /** 읽은 버전과 일치할 때만 변경한다. 전체 교체 INSERT·REPLACE는 사용하지 않는다. */
    @Query("""
        UPDATE expenses SET amount = :amount, dateEpochDay = :dateEpochDay,
            merchant = :merchant, category = :category, paymentType = :paymentType,
            cardId = :cardId, cardName = :cardName, cardIssuer = :cardIssuer,
            memo = :memo, revision = revision + 1
        WHERE id = :id AND revision = :expectedRevision
    """)
    suspend fun update(
        id: String, expectedRevision: Long, amount: Long, dateEpochDay: Long,
        merchant: String?, category: String, paymentType: String,
        cardId: String?, cardName: String?, cardIssuer: String?, memo: String?,
    ): Int

    /** 시작일 포함·종료일 제외. 월별 조회와 기간 검색에 같은 경계를 사용한다. */
    @Query("SELECT * FROM expenses WHERE dateEpochDay >= :startInclusive AND dateEpochDay < :endExclusive ORDER BY dateEpochDay DESC, id DESC")
    fun observeExpenses(startInclusive: Long, endExclusive: Long): Flow<List<ExpenseEntity>>
}
