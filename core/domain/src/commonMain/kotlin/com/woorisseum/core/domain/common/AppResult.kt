package com.woorisseum.core.domain.common

/** 저장 방식과 관계없이 작업의 성공 값 또는 예상 가능한 실패를 전달한다. 로딩은 UI에서 관리한다. */
sealed interface AppResult<out T> {
    /** 작업이 성공했을 때 호출자가 사용할 도메인 값이다. */
    data class Success<T>(val data: T) : AppResult<T>

    /** 외부 SDK 예외 대신 앱에서 정의한 오류를 전달한다. */
    data class Failure(val error: AppError) : AppResult<Nothing>
}
