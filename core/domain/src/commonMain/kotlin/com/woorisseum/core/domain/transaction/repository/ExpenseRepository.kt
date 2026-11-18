package com.woorisseum.core.domain.transaction.repository

import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.core.domain.transaction.model.NewExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth

/**
 * 지출 기능이 사용하는 등록·조회 계약이다. 로컬 DB나 서버를 사용하는 구현은 core:data에 둔다.
 * 예상 가능한 저장소 실패는 AppResult.Failure로 전달하고, 코루틴 취소는 그대로 전파한다.
 */
interface ExpenseRepository {
    /** 지출을 등록하고 식별자가 부여된 결과를 반환한다. 성공한 등록은 목록 구독에도 반영한다. */
    suspend fun addExpense(input: NewExpense): AppResult<Expense>

    /**
     * 구독 시점의 월별 목록과 이후 변경된 전체 목록을 거래 날짜 내림차순으로 전달한다.
     * 내역이 없으면 Success(emptyList())이며, 날짜의 연·월이 month와 일치하는 지출만 포함한다.
     * 예상 가능한 조회 실패는 Failure를 전달한 뒤 종료하며, 재시도는 다시 구독하는 방식이다.
     */
    fun observeExpenses(month: YearMonth): Flow<AppResult<List<Expense>>>
}
