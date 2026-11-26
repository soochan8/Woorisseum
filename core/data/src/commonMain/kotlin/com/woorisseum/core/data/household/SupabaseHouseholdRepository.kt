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
    override suspend fun issueInvitation(requestId: String): AppResult<IssuedInvitation> = authenticated {
        client.postgrest.rpc("create_household_invitation_v1", buildJsonObject { put("p_request_id", requestId) })
            .decodeAs<RpcEnvelope<IssuedInvitationDto>>().result { it.toDomain() }
    }
    override suspend fun previewInvitation(code: InvitationCode): AppResult<InvitationPreview> = authenticated {
        client.postgrest.rpc("preview_household_invitation_v1", buildJsonObject { put("p_token", code.value) })
            .decodeAs<RpcEnvelope<InvitationPreviewDto>>().result { it.toDomain() }
    }
    override suspend fun acceptInvitation(code: InvitationCode): AppResult<Household> = authenticated { userId ->
        client.postgrest.rpc("accept_household_invitation_v1", buildJsonObject { put("p_token", code.value) })
            .decodeAs<RpcEnvelope<AcceptedInvitationDto>>().result { it.toDomain(userId) }
    }
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
        "PROFILE_REQUIRED" -> AppError.Invitation(InvitationFailure.ProfileRequired)
        "INVITATION_NOT_FOUND" -> AppError.Invitation(InvitationFailure.NotFound)
        "SELF_INVITATION" -> AppError.Invitation(InvitationFailure.Self)
        "INVITATION_USED" -> AppError.Invitation(InvitationFailure.Used)
        "INVITATION_REVOKED" -> AppError.Invitation(InvitationFailure.Revoked)
        "INVITATION_EXPIRED" -> AppError.Invitation(InvitationFailure.Expired)
        "HOUSEHOLD_FULL" -> AppError.Invitation(InvitationFailure.Full)
        "ALREADY_CONNECTED" -> AppError.Invitation(InvitationFailure.AlreadyConnected)
        "CURRENT_HOUSEHOLD_HAS_DATA" -> AppError.Invitation(InvitationFailure.CurrentHouseholdHasData)
        "ISSUANCE_RESULT_UNAVAILABLE" -> AppError.Invitation(InvitationFailure.IssuanceResultUnavailable)
        else -> AppError.Remote(RemoteFailure.InvalidResponse)
    })
}
