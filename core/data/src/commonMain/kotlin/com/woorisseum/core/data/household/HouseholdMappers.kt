package com.woorisseum.core.data.household

import com.woorisseum.core.domain.household.*
import kotlin.time.Instant

internal class InvalidHouseholdResponse : Exception("Invalid household response")
private val uuidPattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
private fun verify(value: Boolean) { if (!value) throw InvalidHouseholdResponse() }

internal fun ProfileEmoji.toCode(): String = when (this) {
    ProfileEmoji.Cat -> "CAT"
    ProfileEmoji.Dog -> "DOG"
    ProfileEmoji.Rabbit -> "RABBIT"
    ProfileEmoji.Bear -> "BEAR"
    ProfileEmoji.Fox -> "FOX"
    ProfileEmoji.Panda -> "PANDA"
    ProfileEmoji.Frog -> "FROG"
    ProfileEmoji.Penguin -> "PENGUIN"
}

internal fun ProfileDto.toDomain(): HouseholdProfile {
    verify(uuidPattern.matches(userId) && revision >= 1 && ProfileName.isValid(name) && ProfileName.normalize(name) == name)
    val selection = ProfileEmoji.entries.firstOrNull { it.toCode() == emoji } ?: throw InvalidHouseholdResponse()
    return HouseholdProfile(userId, name, selection, revision)
}

/** 서버의 모순된 응답을 빈 상태·연결 성공으로 바꾸지 않는다. */
internal fun SnapshotDto.toDomain(currentUserId: String): TogetherSnapshot {
    val own = profile?.toDomain()
    verify(own == null || own.userId == currentUserId)
    val home = household?.let { dto ->
        verify(uuidPattern.matches(dto.id))
        val people = dto.members.map { it.toDomain() }
        verify(people.size in 1..2 && people.map { it.userId }.distinct().size == people.size)
        verify(own != null && people.singleOrNull { it.userId == currentUserId } == own)
        Household(dto.id, people)
    }
    val pending = invitation?.let { dto ->
        verify(uuidPattern.matches(dto.id) && dto.status in listOf("AVAILABLE", "EXPIRED"))
        val expires = try { Instant.parse(dto.expiresAt) } catch (_: IllegalArgumentException) { throw InvalidHouseholdResponse() }
        PendingInvitation(dto.id, expires, dto.status == "EXPIRED")
    }
    verify(when (connectionState) {
        "NOT_CONNECTED" -> home == null && pending == null
        "INVITING" -> own != null && home?.members?.size == 1
        "CONNECTED" -> own != null && home?.members?.size == 2 && pending == null
        else -> false
    })
    return TogetherSnapshot(own, home, pending)
}

internal fun IssuedInvitationDto.toDomain(): IssuedInvitation {
    verify(uuidPattern.matches(id) && uuidPattern.matches(householdId))
    val code = InvitationCode.parse(token) ?: throw InvalidHouseholdResponse()
    return IssuedInvitation(id, householdId, code, parseExpiry(expiresAt))
}
internal fun InvitationPreviewDto.toDomain(): InvitationPreview {
    verify(uuidPattern.matches(id) && uuidPattern.matches(householdId) && ProfileName.isValid(inviter.name))
    val emoji = ProfileEmoji.entries.firstOrNull { it.toCode() == inviter.emoji } ?: throw InvalidHouseholdResponse()
    return InvitationPreview(id, householdId, inviter.name, emoji, parseExpiry(expiresAt))
}
internal fun AcceptedInvitationDto.toDomain(userId: String): Household {
    verify(outcome in listOf("JOINED", "ALREADY_JOINED", "ALREADY_CONNECTED") && uuidPattern.matches(household.id))
    val members = household.members.map { it.toDomain() }
    verify(members.size == 2 && members.map { it.userId }.distinct().size == 2 && members.any { it.userId == userId })
    return Household(household.id, members)
}
private fun parseExpiry(value: String): Instant = try { Instant.parse(value) }
catch (_: IllegalArgumentException) { throw InvalidHouseholdResponse() }
