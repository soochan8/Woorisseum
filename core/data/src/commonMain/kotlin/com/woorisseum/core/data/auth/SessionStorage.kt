package com.woorisseum.core.data.auth

/** 플랫폼 보안 저장소 경계. 이름·이모지나 업무 DB와 인증 자격 증명을 분리한다. */
interface SessionStorage {
    suspend fun read(): String?
    suspend fun write(value: String)
    suspend fun clear()
}

internal class SessionStorageException : Exception("Authentication session storage is unavailable")
