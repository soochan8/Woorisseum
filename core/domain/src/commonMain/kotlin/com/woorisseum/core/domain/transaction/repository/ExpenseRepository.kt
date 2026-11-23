package com.woorisseum.core.domain.transaction.repository

import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.core.domain.transaction.model.NewExpense
import com.woorisseum.core.domain.transaction.search.ExpenseSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth

/**
 * 지출 기능이 사용하는 등록·조회 계약이다. 로컬 DB나 서버를 사용하는 구현은 core:data에 둔다.
 * 예상 가능한 저장소 실패는 AppResult.Failure로 전달하고, 코루틴 취소는 그대로 전파한다.
 */
interface ExpenseRepository {
    /**
     * 양의 정수 금액을 등록한다. 미래 날짜는 허용하며 사용처·메모는 공백 정리 후 미입력을 null로 저장한다.
     * 잘못된 금액·선택 불가능한 카드는 InvalidInput이다. 카드 표시값은 현재 등록 정보로 확정한다.
     * 새 ID와 revision 0을 부여하고, 성공한 등록은 목록 구독에도 반영한다.
     * 호출마다 별개의 등록이다. 응답 유실만으로 자동 재시도해 중복 등록하지 않는다.
     */
    suspend fun addExpense(input: NewExpense): AppResult<Expense>

    /** 항목이 없으면 NotFound, 저장소 오류는 Storage로 구분한다. */
    suspend fun getExpense(id: String): AppResult<Expense>

    /**
     * 조회 당시 revision을 비교해 갱신한다. 대상 없음은 NotFound, 버전 불일치는 Conflict다.
     * 성공 시 ID는 유지하고 revision을 1 증가시킨다. 음수·증가 불가 버전은 InvalidInput이다.
     * 원래 카드 ID를 유지하면 과거 카드 스냅샷을 보존하며, 다른 카드는 현재 등록되어 있어야 한다.
     */
    suspend fun updateExpense(id: String, input: NewExpense, expectedRevision: Long): AppResult<Expense>

    /**
     * 기간·검색어·분류·결제수단을 모두 만족하는 목록을 관찰한다. 날짜 종료일은 포함한다.
     * month와 기간이 모두 있으면 교집합이다. start > endInclusive 입력은 InvalidInput이다.
     * 정렬·빈 목록·오류·취소 계약은 월별 관찰과 같다.
     */
    fun observeExpenses(search: ExpenseSearch): Flow<AppResult<List<Expense>>>

    /**
     * 구독 시점의 월별 목록과 이후 변경된 전체 목록을 거래 날짜 내림차순으로 전달한다. 같은 날짜는 ID 내림차순이다.
     * 내역이 없으면 Success(emptyList())이며, 날짜의 연·월이 month와 일치하는 지출만 포함한다.
     * 예상 가능한 조회 실패는 Failure를 전달한 뒤 종료하며, 재시도는 다시 구독하는 방식이다.
     */
    fun observeExpenses(month: YearMonth): Flow<AppResult<List<Expense>>>
}
