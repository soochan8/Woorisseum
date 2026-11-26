package com.woorisseum.core.domain.auth

import com.woorisseum.core.domain.common.AppResult

/** 인증 토큰과 SDK 타입은 data에 두고, 사용자 식별자만 상위 계층에 전달한다. */
data class UserSession(val userId: String)

interface SessionRepository {
    /** 저장된 사용자를 우선 복원한다. 기존 인증이 실패하면 다른 사용자로 대체하지 않는다. */
    suspend fun ensureSession(): AppResult<UserSession>
}
