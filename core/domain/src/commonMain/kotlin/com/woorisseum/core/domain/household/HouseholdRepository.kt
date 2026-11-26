package com.woorisseum.core.domain.household

import com.woorisseum.core.domain.common.AppResult
import kotlin.time.Instant

/** 표시용 글리프와 외부 SDK에 독립적인 프로필 이모지 선택이다. */
enum class ProfileEmoji { Cat, Dog, Rabbit, Bear, Fox, Panda, Frog, Penguin }
data class HouseholdProfile(val userId: String, val name: String, val emoji: ProfileEmoji, val revision: Long)
data class Household(val id: String, val members: List<HouseholdProfile>)
data class PendingInvitation(val id: String, val expiresAt: Instant, val expired: Boolean)
data class TogetherSnapshot(
    val profile: HouseholdProfile?,
    val household: Household?,
    val invitation: PendingInvitation?,
) {
    val connected: Boolean get() = household?.members?.size == 2
}

interface HouseholdRepository {
    suspend fun issueInvitation(requestId: String): AppResult<IssuedInvitation>
    suspend fun previewInvitation(code: InvitationCode): AppResult<InvitationPreview>
    suspend fun acceptInvitation(code: InvitationCode): AppResult<Household>
    suspend fun load(): AppResult<TogetherSnapshot>
    suspend fun saveProfile(name: String, emoji: ProfileEmoji, expectedRevision: Long?): AppResult<HouseholdProfile>
}
