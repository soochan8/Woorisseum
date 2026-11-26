package com.woorisseum.feature.household.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.auth.SessionRepository
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.common.AppError
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.woorisseum.feature.household.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 프로필 입력 후 인증 세션을 준비한다. 인증 성공과 두 사람의 연결 성공을 구분한다. */
class TogetherViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    private var authenticationJob: Job? = null
    private val mutableState = MutableStateFlow(TogetherUiState())
    val state = mutableState.asStateFlow()

    fun onIntent(intent: TogetherUiIntent) {
        if (intent == TogetherUiIntent.ContinueProfile) {
            continueProfile()
            return
        }
        if (intent == TogetherUiIntent.Back) authenticationJob?.cancel()
        else if (mutableState.value.isAuthenticating) return
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
                TogetherUiIntent.ContinueProfile -> state
                TogetherUiIntent.RequestInvite -> if (state.step == TogetherStep.Invite) state.copy(showPreparation = true) else state
                TogetherUiIntent.CheckInvitation -> if (state.step == TogetherStep.Receive) {
                    state.copy(linkSubmitted = true, showPreparation = state.link.isNotBlank())
                } else state
                TogetherUiIntent.AcceptInvitation -> if (state.step == TogetherStep.Confirmation &&
                    state.invitation?.status == InvitationStatus.Available) state.copy(showPreparation = true) else state
                TogetherUiIntent.Back -> when (state.step) {
                    TogetherStep.Start -> state
                    TogetherStep.Profile -> state.copy(step = TogetherStep.Start, showPreparation = false, isAuthenticating = false, sessionError = null)
                    TogetherStep.Invite, TogetherStep.Receive -> state.copy(step = TogetherStep.Profile, showPreparation = false)
                    TogetherStep.Confirmation -> state.copy(step = TogetherStep.Receive, invitation = null, showPreparation = false)
                    TogetherStep.Connected -> state.copy(step = TogetherStep.Start, invitation = null)
                }
            }
        }
    }

    private fun continueProfile() {
        val current = mutableState.value
        if (current.step != TogetherStep.Profile || current.isAuthenticating) return
        if (current.name.isBlank() || current.emoji == null || current.role == null) {
            mutableState.update { it.copy(profileSubmitted = true) }
            return
        }
        mutableState.update { it.copy(isAuthenticating = true, sessionError = null, profileSubmitted = false) }
        authenticationJob = viewModelScope.launch {
            when (val result = sessionRepository.ensureSession()) {
                is AppResult.Success -> mutableState.update {
                    it.copy(name = it.name.trim(), isAuthenticating = false,
                        step = if (it.role == TogetherRole.Inviter) TogetherStep.Invite else TogetherStep.Receive)
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(isAuthenticating = false, sessionError =
                        if (result.error == AppError.InvalidInput) SessionError.Configuration else SessionError.Unavailable)
                }
            }
        }
    }
}
