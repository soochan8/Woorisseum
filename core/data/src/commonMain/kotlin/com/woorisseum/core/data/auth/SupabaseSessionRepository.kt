package com.woorisseum.core.data.auth

import com.woorisseum.core.domain.common.RemoteFailure
import com.woorisseum.core.data.remote.toRemoteError
import com.woorisseum.core.domain.auth.SessionRepository
import com.woorisseum.core.domain.auth.UserSession
import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.io.IOException
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 인증 생성 경합을 직렬화한다. 네트워크·복원 실패 때 새 ID를 발급하지 않는다. */
internal class SupabaseSessionRepository(private val client: SupabaseClient) : SessionRepository {
    private val mutex = Mutex()

    override suspend fun ensureSession(): AppResult<UserSession> = mutex.withLock {
        try {
            val auth = client.auth
            auth.awaitInitialization()
            if (auth.currentSessionOrNull() == null) {
                val stored = auth.sessionManager.loadSessionOrNull()
                if (stored == null) auth.signInAnonymously()
                else auth.importSession(stored, autoRefresh = false, source = SessionSource.Storage)
            }
            // 세션 갱신 결과를 기다린 뒤 반환한다. 오프라인 실패를 신규 가입으로 우회하지 않는다.
            if (auth.currentSessionOrNull()?.expiresAt?.let { it <= Clock.System.now() } == true) {
                auth.refreshCurrentSession()
            }
            val session = auth.currentSessionOrNull()
            val userId = session?.user?.id
            if (session == null || userId.isNullOrBlank()) AppResult.Failure(AppError.NotFound)
            else {
                auth.sessionManager.saveSession(session)
                AppResult.Success(UserSession(userId))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SessionStorageException) {
            AppResult.Failure(AppError.Storage)
        } catch (failure: RestException) {
            AppResult.Failure(failure.toRemoteError())
        } catch (_: HttpRequestException) {
            AppResult.Failure(AppError.Remote(RemoteFailure.Network))
        } catch (_: HttpRequestTimeoutException) {
            AppResult.Failure(AppError.Remote(RemoteFailure.Network))
        } catch (_: IOException) {
            AppResult.Failure(AppError.Remote(RemoteFailure.Network))
        }
    }
}
