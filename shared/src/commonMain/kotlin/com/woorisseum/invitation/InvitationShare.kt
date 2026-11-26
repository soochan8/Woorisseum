package com.woorisseum.invitation

import androidx.compose.runtime.Composable

/** 공유 시트는 플랫폼이 제공하고 초대 생성·수락은 공통 계층이 담당한다. */
@Composable
internal expect fun rememberInvitationShare(): (String) -> Unit
