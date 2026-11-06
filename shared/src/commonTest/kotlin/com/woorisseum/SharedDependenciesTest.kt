package com.woorisseum

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks the shared DI, coroutine, date and serialization dependencies on both platforms. */
class SharedDependenciesTest {
    @Test
    fun sharedLibrariesWorkTogether() = runTest {
        val application = koinApplication {
            modules(module { single { Json { ignoreUnknownKeys = true } } })
        }
        try {
            val json = application.koin.get<Json>()
            val payload = flowOf("""{"date":"2026-11-06","amount":12000,"extra":true}""")
                .map { json.decodeFromString<DependencyPayload>(it) }
                .first()

            assertEquals(LocalDate(2026, 11, 6), payload.date)
            assertEquals(12000L, payload.amount)
            assertEquals(payload, json.decodeFromString<DependencyPayload>(json.encodeToString(payload)))
        } finally {
            application.close()
        }
    }
}

@Serializable
private data class DependencyPayload(val date: LocalDate, val amount: Long)
