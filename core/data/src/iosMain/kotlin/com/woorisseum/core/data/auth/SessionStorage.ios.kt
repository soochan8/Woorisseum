package com.woorisseum.core.data.auth

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.dsl.module

fun sessionStorageModule(projectUrl: String) = module {
    single<SessionStorage> { IosSessionStorage(projectUrl) }
}

/** 다른 프로젝트의 세션을 복원하지 않도록 Keychain 서비스 범위를 나눈다. */
@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
private class IosSessionStorage(projectUrl: String) : SessionStorage {
    private val settings = KeychainSettings(service = "com.woorisseum.auth.$projectUrl")
    override suspend fun read(): String? = access { settings.getStringOrNull("session") }
    override suspend fun write(value: String) = access { settings.putString("session", value) }
    override suspend fun clear() = access { settings.remove("session") }

    private suspend fun <T> access(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (_: IllegalStateException) {
            throw SessionStorageException()
        }
    }
}
