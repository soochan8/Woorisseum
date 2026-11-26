package com.woorisseum.core.data.remote

import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.RemoteFailure
import io.github.jan.supabase.exceptions.RestException

/** HTTP 오류 본문·토큰을 상위 계층이나 로그에 전달하지 않는다. */
internal fun RestException.toRemoteError(): AppError = AppError.Remote(when (statusCode) {
    401 -> RemoteFailure.Authentication
    403 -> RemoteFailure.Permission
    429 -> RemoteFailure.RateLimited
    else -> RemoteFailure.Unavailable
})
