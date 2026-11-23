package com.woorisseum.core.domain.common

/** data가 외부 구현의 실패를 변환하는 공통 오류 계약이다. 표시 문구는 feature가 결정한다. */
sealed interface AppError {
    /** 저장소에서 데이터를 읽거나 기록하지 못한 경우다. */
    data object Storage : AppError

    /** 조회·수정하려는 대상이 없다. */
    data object NotFound : AppError

    /** 조회 이후 다른 수정이 반영되어 현재 초안으로 덮어쓸 수 없다. */
    data object Conflict : AppError

    /** 업무 규칙에 맞지 않는 입력이나 더 이상 선택할 수 없는 참조다. */
    data object InvalidInput : AppError
}
