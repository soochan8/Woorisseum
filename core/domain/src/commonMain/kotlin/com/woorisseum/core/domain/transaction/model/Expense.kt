package com.woorisseum.core.domain.transaction.model

import kotlinx.datetime.LocalDate

/**
 * 저장·조회한 지출을 나타낸다. DB Entity나 화면의 표시 형식에 의존하지 않는다.
 *
 * @property id 저장 구현에서 부여하는 지출 식별자.
 * @property amount 부동소수점 오차 없이 전달하는 정수 금액. 통화·허용 범위는 입력 정책에서 정한다.
 * @property date 시각·시간대가 아닌 사용자가 지정한 거래 날짜. 월별 조회도 이 날짜를 기준으로 한다.
 * @property merchant 사용처 정보가 없는 경우 null. 필수 여부는 별도 입력 정책에서 정한다.
 */
data class Expense(
    val id: String,
    val amount: Long,
    val date: LocalDate,
    val merchant: String?,
    val category: ExpenseCategory,
    val paymentMethod: PaymentMethod,
    /** 선택 입력 메모. 미입력은 null로 전달한다. */
    val memo: String? = null,
)
