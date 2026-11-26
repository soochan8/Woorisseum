package com.woorisseum.invitation

import com.woorisseum.core.domain.household.InvitationCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** OS에서 받은 초대를 화면에 전달할 때까지만 메모리에 보관한다. URL을 로그·저장 상태에 남기지 않는다. */
object InvitationLinks {
    private val mutablePending = MutableStateFlow<IncomingInvitation?>(null)
    internal val pending = mutablePending.asStateFlow()
    private var nextId = 0L

    fun open(url: String): Boolean {
        val raw = Regex("woorisseum://invite\\?code=([A-Za-z0-9_-]{43})").matchEntire(url)?.groupValues?.get(1) ?: return false
        val code = InvitationCode.parse(raw) ?: return false
        mutablePending.value = IncomingInvitation(++nextId, code)
        return true
    }

    internal fun handled(id: Long) {
        mutablePending.compareAndSet(mutablePending.value?.takeIf { it.id == id }, null)
    }

    internal fun create(code: String): String? = InvitationCode.parse(code)?.let { "woorisseum://invite?code=${it.value}" }
}

internal class IncomingInvitation(val id: Long, val code: InvitationCode)
