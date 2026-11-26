package com.woorisseum.feature.household.model

/** 서버 연동 전의 입력 초안이다. 연결·저장된 프로필을 의미하지 않는다. */
data class TogetherUiState(
    val step: TogetherStep = TogetherStep.Start,
    val role: TogetherRole? = null,
    val name: String = "",
    val emoji: TogetherEmoji? = null,
    val link: String = "",
    val profileSubmitted: Boolean = false,
    val linkSubmitted: Boolean = false,
    val showPreparation: Boolean = false,
    val isAuthenticating: Boolean = false,
    val sessionError: SessionError? = null,
    val invitation: InvitationDisplay? = null,
)

enum class SessionError { Configuration, Unavailable }

enum class TogetherStep { Start, Profile, Invite, Receive, Confirmation, Connected }
enum class TogetherRole { Inviter, Invitee }
enum class TogetherEmoji { Cat, Dog, Rabbit, Bear, Fox, Panda, Frog, Penguin }
enum class InvitationStatus { Available, Expired, Used, Full }

/** 서버 확인 결과를 그릴 표시 모델. 현재는 Preview에서만 주입한다. */
data class InvitationDisplay(
    val inviterName: String,
    val inviterEmoji: TogetherEmoji,
    val status: InvitationStatus,
)

sealed interface TogetherUiIntent {
    data class ChooseRole(val role: TogetherRole) : TogetherUiIntent
    data class ChangeName(val value: String) : TogetherUiIntent
    data class SelectEmoji(val emoji: TogetherEmoji) : TogetherUiIntent
    data class ChangeLink(val value: String) : TogetherUiIntent
    data object ContinueProfile : TogetherUiIntent
    data object RequestInvite : TogetherUiIntent
    data object CheckInvitation : TogetherUiIntent
    data object AcceptInvitation : TogetherUiIntent
    data object Back : TogetherUiIntent
}
