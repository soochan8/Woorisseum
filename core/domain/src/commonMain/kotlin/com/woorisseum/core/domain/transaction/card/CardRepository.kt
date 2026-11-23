package com.woorisseum.core.domain.transaction.card

import com.woorisseum.core.domain.common.AppResult
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    /**
     * 현재 목록부터 등록 순서대로 전달하고 이후 변경을 반영한다.
     * 빈 목록은 Success(emptyList), 예상 가능한 조회 실패는 Failure를 전달한 뒤 종료한다.
     * 재시도는 새로 구독하며 비동기 취소는 실패 값으로 변환하지 않는다.
     */
    fun observeCards(): Flow<AppResult<List<RegisteredCard>>>

    /** 입력은 앞뒤 공백을 제거한 필수값이어야 한다. 같은 별칭은 허용한다. */
    suspend fun addCard(alias: String, issuer: String): AppResult<RegisteredCard>

    /** ID를 유지하며 수정한다. 존재하지 않는 ID는 실패하고 새 카드를 만들지 않는다. */
    suspend fun updateCard(id: String, alias: String, issuer: String): AppResult<RegisteredCard>
    /** 카드 등록 정보만 삭제한다. 과거 지출은 수정하지 않으며 이미 없는 ID의 삭제도 성공한다. */
    suspend fun deleteCard(id: String): AppResult<Unit>
}
