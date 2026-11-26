package com.woorisseum.core.domain.household

import kotlin.time.Instant

/** 초대 비밀 값은 메모리에서만 보관하며 로그 문자열에 노출하지 않는다. */
class InvitationCode private constructor(val value: String) {
    override fun toString(): String = "InvitationCode([redacted])"
    companion object {
        fun parse(input: String): InvitationCode? = input.trim().takeIf {
            Regex("[A-Za-z0-9_-]{42}[AEIMQUYcgkosw048]").matches(it)
        }?.let(::InvitationCode)
    }
}
data class IssuedInvitation(val id: String, val householdId: String, val code: InvitationCode, val expiresAt: Instant)
data class InvitationPreview(val id: String, val householdId: String, val inviterName: String, val inviterEmoji: ProfileEmoji, val expiresAt: Instant)
