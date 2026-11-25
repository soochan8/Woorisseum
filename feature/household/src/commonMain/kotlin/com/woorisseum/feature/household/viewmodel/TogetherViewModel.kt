package com.woorisseum.feature.household.viewmodel

import androidx.lifecycle.ViewModel
import com.woorisseum.feature.household.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 역할별 화면 전이와 입력 검증만 담당한다. 링크 발급·참여 성공을 흉내 내지 않는다. */
class TogetherViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(TogetherUiState())
    val state = mutableState.asStateFlow()

    fun onIntent(intent: TogetherUiIntent) {
        mutableState.update { state ->
            when (intent) {
                is TogetherUiIntent.ChooseRole -> if (state.step == TogetherStep.Start) {
                    TogetherUiState(step = TogetherStep.Profile, role = intent.role,
                        name = state.name, emoji = state.emoji)
                } else state
                is TogetherUiIntent.ChangeName -> if (state.step == TogetherStep.Profile) state.copy(name = intent.value) else state
                is TogetherUiIntent.SelectEmoji -> if (state.step == TogetherStep.Profile) state.copy(emoji = intent.emoji) else state
                is TogetherUiIntent.ChangeLink -> if (state.step == TogetherStep.Receive) {
                    state.copy(link = intent.value, linkSubmitted = false, showPreparation = false)
                } else state
                TogetherUiIntent.ContinueProfile -> if (state.step == TogetherStep.Profile) {
                    if (state.name.isBlank() || state.emoji == null || state.role == null) state.copy(profileSubmitted = true)
                    else state.copy(name = state.name.trim(), profileSubmitted = false,
                        step = if (state.role == TogetherRole.Inviter) TogetherStep.Invite else TogetherStep.Receive)
                } else state
                TogetherUiIntent.RequestInvite -> if (state.step == TogetherStep.Invite) state.copy(showPreparation = true) else state
                TogetherUiIntent.CheckInvitation -> if (state.step == TogetherStep.Receive) {
                    state.copy(linkSubmitted = true, showPreparation = state.link.isNotBlank())
                } else state
                TogetherUiIntent.AcceptInvitation -> if (state.step == TogetherStep.Confirmation &&
                    state.invitation?.status == InvitationStatus.Available) state.copy(showPreparation = true) else state
                TogetherUiIntent.Back -> when (state.step) {
                    TogetherStep.Start -> state
                    TogetherStep.Profile -> state.copy(step = TogetherStep.Start, showPreparation = false)
                    TogetherStep.Invite, TogetherStep.Receive -> state.copy(step = TogetherStep.Profile, showPreparation = false)
                    TogetherStep.Confirmation -> state.copy(step = TogetherStep.Receive, invitation = null, showPreparation = false)
                    TogetherStep.Connected -> state.copy(step = TogetherStep.Start, invitation = null)
                }
            }
        }
    }
}
