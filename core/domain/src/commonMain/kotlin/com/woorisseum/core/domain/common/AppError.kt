package com.woorisseum.core.domain.common

/** data가 외부 구현의 실패를 변환하는 공통 오류 계약이다. 표시 문구는 feature가 결정한다. */
sealed interface AppError {
    /** 저장소에서 데이터를 읽거나 기록하지 못한 경우다. */
    data object Storage : AppError
}
