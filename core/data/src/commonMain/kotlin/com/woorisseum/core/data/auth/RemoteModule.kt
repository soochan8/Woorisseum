package com.woorisseum.core.data.auth

import com.woorisseum.core.domain.auth.SessionRepository
import com.woorisseum.core.domain.household.*
import com.woorisseum.core.domain.common.RemoteFailure
import com.woorisseum.core.data.household.SupabaseHouseholdRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlin.time.Duration.Companion.seconds
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
    single {
        createSupabaseClient(config.url, config.publishableKey) {
            defaultLogLevel = LogLevel.NONE
            install(Auth) {
                sessionManager = SecureSessionManager(get())
                autoLoadFromStorage = false
                alwaysAutoRefresh = false
                enableLifecycleCallbacks = false
            }
            install(Postgrest) { timeout = 15.seconds; maxRetries = 0 }
        }
    }
    single<SessionRepository> {
        if (config.isConfigured) SupabaseSessionRepository(get<SupabaseClient>()) else UnconfiguredSessionRepository()
    }
    single<HouseholdRepository> {
        if (config.isConfigured) SupabaseHouseholdRepository(get(), get()) else UnconfiguredHouseholdRepository()
    }
}

private class UnconfiguredSessionRepository : SessionRepository {
    override suspend fun ensureSession() = AppResult.Failure(AppError.Remote(RemoteFailure.Configuration))
}

private class UnconfiguredHouseholdRepository : HouseholdRepository {
    override suspend fun load(): AppResult<TogetherSnapshot> = AppResult.Failure(AppError.Remote(RemoteFailure.Configuration))
    override suspend fun saveProfile(name: String, emoji: ProfileEmoji, expectedRevision: Long?): AppResult<HouseholdProfile> =
        AppResult.Failure(AppError.Remote(RemoteFailure.Configuration))
}
