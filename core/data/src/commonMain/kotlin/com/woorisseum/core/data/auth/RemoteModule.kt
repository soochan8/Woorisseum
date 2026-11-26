package com.woorisseum.core.data.auth

import com.woorisseum.core.domain.auth.SessionRepository
import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import org.koin.dsl.module

/** 공개 앱 접속 정보만 받는다. 비밀 키나 사용자 토큰을 생성자 설정으로 전달하지 않는다. */
class SupabaseConfig(val url: String, val publishableKey: String) {
    internal val isConfigured: Boolean
        get() = runCatching { Url(url).protocol == URLProtocol.HTTPS && Url(url).host.isNotBlank() }
            .getOrDefault(false) && publishableKey.startsWith("sb_publishable_")
}

fun remoteModule(config: SupabaseConfig) = module {
    single<SessionRepository> {
        if (!config.isConfigured) UnconfiguredSessionRepository()
        else {
            val manager = SecureSessionManager(get())
            val client = createSupabaseClient(config.url, config.publishableKey) {
                defaultLogLevel = LogLevel.NONE
                install(Auth) {
                    sessionManager = manager
                    // 복원 오류를 SDK가 세션 없음으로 처리하지 않도록 Repository가 복원을 소유한다.
                    autoLoadFromStorage = false
                    // 생명주기 콜백의 자동 재조회도 끄고 이 Repository가 복원·갱신을 소유한다.
                    alwaysAutoRefresh = false
                    enableLifecycleCallbacks = false
                }
            }
            SupabaseSessionRepository(client)
        }
    }
}

private class UnconfiguredSessionRepository : SessionRepository {
    override suspend fun ensureSession() = AppResult.Failure(AppError.InvalidInput)
}
