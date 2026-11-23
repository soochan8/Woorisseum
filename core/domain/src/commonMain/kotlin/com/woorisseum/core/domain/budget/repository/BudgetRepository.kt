package com.woorisseum.core.domain.budget.repository

import com.woorisseum.core.domain.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth

/** 월별 예산 계약. 미설정 null과 설정한 0원을 구분한다. */
interface BudgetRepository {
    /** 현재 값과 이후 변경을 관찰한다. 조회 실패는 Failure 후 종료하며 재구독으로 재시도한다. */
    fun observeBudget(month: YearMonth): Flow<AppResult<Long?>>

    /** 월별 한 개의 예산을 원자적으로 저장한다. 음수는 InvalidInput, 저장 오류는 Storage다. */
    suspend fun saveBudget(month: YearMonth, amount: Long): AppResult<Unit>
}
