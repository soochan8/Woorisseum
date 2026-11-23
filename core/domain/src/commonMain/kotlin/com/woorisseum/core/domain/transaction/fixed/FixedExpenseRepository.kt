package com.woorisseum.core.domain.transaction.fixed

import com.woorisseum.core.domain.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth

/** 매월 반복 계획의 저장 계약. 납부·실제 지출을 생성하지 않는다. */
interface FixedExpenseRepository {
    /** 해당 월에 유효한 계획만 한 번씩 전달한다. 조회 오류는 Failure 후 종료하며 재구독으로 재시도한다. */
    fun observePlans(month: YearMonth): Flow<AppResult<List<FixedExpensePlan>>>
    suspend fun getPlan(id: String, month: YearMonth): AppResult<FixedExpensePlan>
    suspend fun addPlan(startMonth: YearMonth, input: FixedExpenseInput): AppResult<FixedExpensePlan>
    /** 조회 당시 revision으로 변경한다. 이전 월은 보존하고 적용 월에 새 이력을 기록한다. */
    suspend fun updatePlan(id: String, fromMonth: YearMonth, input: FixedExpenseInput, expectedRevision: Long): AppResult<FixedExpensePlan>
    /** 해당 월부터 예정 목록에서 제외한다. 과거 이력은 삭제하지 않는다. */
    suspend fun stopPlan(id: String, fromMonth: YearMonth, expectedRevision: Long): AppResult<Unit>
}

/** 확정된 계획 입력. 시작·적용 월과 식별자는 저장 명령에서 별도로 전달한다. */
data class FixedExpenseInput(
    val type: FixedExpenseType, val name: String?, val amount: Long,
    val billingDay: Int, val memo: String? = null,
)

fun FixedExpenseInput.isValid(): Boolean = amount > 0L && billingDay in 1..31 &&
    (type != FixedExpenseType.OTHER || !name.isNullOrBlank())
