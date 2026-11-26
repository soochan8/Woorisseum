package com.woorisseum.core.data.household

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable internal data class RpcEnvelope<T>(val ok: Boolean, val data: T? = null, val error: RpcError? = null)
@Serializable internal data class RpcError(val code: String, val field: String? = null)
@Serializable internal data class ProfileResultDto(val profile: ProfileDto)
@Serializable internal data class ProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val name: String,
    @SerialName("emoji_code") val emoji: String,
    val revision: Long,
)
@Serializable internal data class SnapshotDto(
    val profile: ProfileDto?,
    @SerialName("connection_state") val connectionState: String,
    val household: HouseholdDto?,
    val invitation: PendingInvitationDto?,
)
@Serializable internal data class HouseholdDto(
    @SerialName("household_id") val id: String,
    val members: List<ProfileDto>,
)
@Serializable internal data class PendingInvitationDto(
    @SerialName("invitation_id") val id: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String,
)
