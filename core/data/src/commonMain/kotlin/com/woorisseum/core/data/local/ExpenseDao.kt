package com.woorisseum.core.data.local

import androidx.room3.Dao
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ExpenseDao {
    /** 시작일 포함·종료일 제외. 월별 조회와 기간 검색에 같은 경계를 사용한다. */
    @Query("SELECT * FROM expenses WHERE dateEpochDay >= :startInclusive AND dateEpochDay < :endExclusive ORDER BY dateEpochDay DESC, id DESC")
    fun observeExpenses(startInclusive: Long, endExclusive: Long): Flow<List<ExpenseEntity>>
}
