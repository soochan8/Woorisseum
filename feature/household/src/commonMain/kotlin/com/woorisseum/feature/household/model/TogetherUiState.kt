package com.woorisseum.feature.household.model

import com.woorisseum.core.domain.household.*

/** 입력 초안과 마지막으로 확인한 서버 상태를 구분한다. 조회 실패를 미연결로 바꾸지 않는다. */
data class TogetherUiState(
    val step: TogetherStep = TogetherStep.Start,
    val role: TogetherRole? = null,
    val name: String = "",
    val emoji: TogetherEmoji? = null,
    val codeInput: String = "",
    val profileSubmitted: Boolean = false,
    val codeSubmitted: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val snapshot: TogetherSnapshot? = null,
    val operationError: TogetherOperationError? = null,
    val savedAwaitingRefresh: Boolean = false,
    val hasConflict: Boolean = false,
    val conflictReady: Boolean = false,
    val invitation: InvitationDisplay? = null,
    val issued: IssuedInvitation? = null,
    val checked: InvitationPreview? = null,
    val reissueConfirmation: Boolean = false,
    val issuanceUncertain: Boolean = false,
    val acceptanceUncertain: Boolean = false,
    val expired: Boolean = false,
    val invitationFailure: com.woorisseum.core.domain.common.InvitationFailure? = null,
)

enum class TogetherOperationError { Configuration, Network, Authentication, Permission, RateLimited, Unavailable, InvalidResponse, Conflict, InvalidInput, Storage }

enum class TogetherStep { Start, Profile, Invite, Receive, Confirmation, Connected }
enum class TogetherRole { Inviter, Invitee }
typealias TogetherEmoji = ProfileEmoji
enum class InvitationStatus { Available, Expired, Used, Full }

/** 초대 확인 결과를 그릴 표시 모델. */
data class InvitationDisplay(
    val inviterName: String,
    val inviterEmoji: TogetherEmoji,
    val status: InvitationStatus,
)

sealed interface TogetherUiIntent {
    data class ChooseRole(val role: TogetherRole) : TogetherUiIntent
    data class ChangeName(val value: String) : TogetherUiIntent
    data class SelectEmoji(val emoji: TogetherEmoji) : TogetherUiIntent
    data class OpenInvitation(val code: String) : TogetherUiIntent
    data class ChangeCode(val value: String) : TogetherUiIntent
    data object Refresh : TogetherUiIntent
    data object EditProfile : TogetherUiIntent
    data object UseServerProfile : TogetherUiIntent
    data object SaveDraftOverLatest : TogetherUiIntent
    data object ContinueProfile : TogetherUiIntent
    data object Tick : TogetherUiIntent
    data object ConfirmReissue : TogetherUiIntent
    data object CancelReissue : TogetherUiIntent
    data object RequestInvite : TogetherUiIntent
    data object CheckInvitation : TogetherUiIntent
    data object AcceptInvitation : TogetherUiIntent
    data object Back : TogetherUiIntent
}
