package com.woorisseum.core.data.household

import com.woorisseum.core.data.remote.toRemoteError
import com.woorisseum.core.domain.auth.SessionRepository
import com.woorisseum.core.domain.common.*
import com.woorisseum.core.domain.household.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal class SupabaseHouseholdRepository(
    private val client: SupabaseClient,
    private val sessions: SessionRepository,
) : HouseholdRepository {
    override suspend fun load(): AppResult<TogetherSnapshot> = authenticated { userId ->
        val envelope = client.postgrest.rpc("get_together_state_v1").decodeAs<RpcEnvelope<SnapshotDto>>()
        envelope.result { it.toDomain(userId) }
    }

    override suspend fun saveProfile(name: String, emoji: ProfileEmoji, expectedRevision: Long?): AppResult<HouseholdProfile> {
        if (!ProfileName.isValid(name) || (expectedRevision != null && expectedRevision < 1)) return AppResult.Failure(AppError.InvalidInput)
        return authenticated { userId ->
            val input = buildJsonObject {
                put("p_display_name", ProfileName.normalize(name))
                put("p_emoji_code", emoji.toCode())
                put("p_expected_revision", expectedRevision?.let(::JsonPrimitive) ?: JsonNull)
            }
            val envelope = client.postgrest.rpc("save_my_profile_v1", input).decodeAs<RpcEnvelope<ProfileResultDto>>()
            envelope.result { dto ->
                dto.profile.toDomain().also { if (it.userId != userId || it.name != ProfileName.normalize(name) || it.emoji != emoji) throw InvalidHouseholdResponse() }
            }
        }
    }

    private suspend fun <T> authenticated(action: suspend (String) -> AppResult<T>): AppResult<T> {
        return try {
            when (val session = sessions.ensureSession()) {
                is AppResult.Failure -> session
                is AppResult.Success -> action(session.data.userId)
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: RestException) { AppResult.Failure(failure.toRemoteError())
        } catch (_: HttpRequestException) { AppResult.Failure(AppError.Remote(RemoteFailure.Network))
        } catch (_: HttpRequestTimeoutException) { AppResult.Failure(AppError.Remote(RemoteFailure.Network))
        } catch (_: IOException) { AppResult.Failure(AppError.Remote(RemoteFailure.Network))
        } catch (_: SerializationException) { AppResult.Failure(AppError.Remote(RemoteFailure.InvalidResponse))
        } catch (_: InvalidHouseholdResponse) { AppResult.Failure(AppError.Remote(RemoteFailure.InvalidResponse)) }
    }
}

private fun <T, R> RpcEnvelope<T>.result(mapper: (T) -> R): AppResult<R> {
    if (ok) {
        if (data == null || error != null) throw InvalidHouseholdResponse()
        return AppResult.Success(mapper(data))
    }
    if (data != null || error == null) throw InvalidHouseholdResponse()
    return AppResult.Failure(when (error.code) {
        "PROFILE_CONFLICT" -> AppError.Conflict
        "INVALID_ARGUMENT" -> AppError.InvalidInput
        else -> AppError.Remote(RemoteFailure.InvalidResponse)
    })
}
