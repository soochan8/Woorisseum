package com.woorisseum.core.data.auth

import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** 저장 실패를 '세션 없음'으로 숨기지 않는다. 재시도는 같은 발급 세션을 보관해 사용한다. */
internal class SecureSessionManager(private val storage: SessionStorage) : SessionManager {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private var cached: UserSession? = null

    override suspend fun saveSession(session: UserSession) = mutex.withLock {
        cached = session
        storage.write(json.encodeToString(session))
    }

    override suspend fun loadSessionOrNull(): UserSession? = mutex.withLock {
        cached ?: storage.read()?.let { value ->
            try {
                json.decodeFromString<UserSession>(value).also { cached = it }
            } catch (_: SerializationException) {
                throw SessionStorageException()
            } catch (_: IllegalArgumentException) {
                throw SessionStorageException()
            }
        }
    }

    override suspend fun loadSession(): UserSession = loadSessionOrNull() ?: throw NoSessionFoundException()

    override suspend fun deleteSession() = mutex.withLock {
        storage.clear()
        cached = null
    }
}
