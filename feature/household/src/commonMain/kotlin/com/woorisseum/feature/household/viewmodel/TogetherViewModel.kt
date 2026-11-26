package com.woorisseum.feature.household.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.common.*
import com.woorisseum.core.domain.household.*
import com.woorisseum.feature.household.model.*
import kotlin.uuid.Uuid
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 서버 결과에 따른 화면 전이를 소유한다. 저장 성공 후 조회 실패와 저장 실패를 구분한다. */
class TogetherViewModel(private val repository: HouseholdRepository) : ViewModel() {
    private var work: Job? = null
    private var generation = 0L
    private val mutableState = MutableStateFlow(TogetherUiState())
    val state = mutableState.asStateFlow()

    init { refresh() }

    fun onIntent(intent: TogetherUiIntent) {
        if (intent == TogetherUiIntent.Tick) {
            mutableState.update { val expiry = if (it.step == TogetherStep.Confirmation) it.checked?.expiresAt else it.issued?.expiresAt
                it.copy(expired = expiry?.let { end -> Clock.System.now() >= end } == true) }
            return
        }
        if (intent == TogetherUiIntent.Back) {
            if (state.value.isSaving) return
            generation++
            work?.cancel()
            mutableState.update { current ->
                current.copy(isLoading = false, isSaving = false,
                    role = if (current.step == TogetherStep.Profile) null else current.role,
                    operationError = if (current.hasConflict) TogetherOperationError.Conflict else current.operationError,
                    step = when (current.step) {
                        TogetherStep.Profile -> if (current.snapshot?.connected == true) TogetherStep.Connected else TogetherStep.Start
                        TogetherStep.Invite, TogetherStep.Receive -> TogetherStep.Profile
                        TogetherStep.Confirmation -> TogetherStep.Receive
                        else -> current.step
                    })
            }
            return
        }
        if (mutableState.value.isLoading || mutableState.value.isSaving) return
        when (intent) {
            is TogetherUiIntent.OpenInvitation -> {
                val code = InvitationCode.parse(intent.code) ?: return
                val current = state.value
                if (current.snapshot == null) return
                if (current.snapshot.connected) {
                    mutableState.update { it.copy(step = TogetherStep.Connected, invitationFailure = InvitationFailure.AlreadyConnected) }
                } else {
                    mutableState.update { it.copy(role = TogetherRole.Invitee, codeInput = code.value,
                        step = if (current.snapshot.profile == null || current.step == TogetherStep.Profile) TogetherStep.Profile else TogetherStep.Receive,
                        checked = null, invitation = null, codeSubmitted = false, invitationFailure = null, operationError = null,
                        reissueConfirmation = false, acceptanceUncertain = false, expired = false) }
                    if (state.value.step == TogetherStep.Receive) checkInvitation()
                }
            }
            TogetherUiIntent.RequestInvite -> if (state.value.step == TogetherStep.Invite) {
                if (state.value.issued != null || state.value.snapshot?.invitation != null || state.value.issuanceUncertain)
                    mutableState.update { it.copy(reissueConfirmation = true) }
                else issueInvitation()
            }
            TogetherUiIntent.ConfirmReissue -> if (state.value.reissueConfirmation) issueInvitation()
            TogetherUiIntent.CancelReissue -> mutableState.update { it.copy(reissueConfirmation = false) }
            TogetherUiIntent.CheckInvitation -> checkInvitation()
            TogetherUiIntent.AcceptInvitation -> acceptInvitation()
            TogetherUiIntent.Refresh -> refresh()
            TogetherUiIntent.ContinueProfile -> continueProfile()
            TogetherUiIntent.UseServerProfile -> if (state.value.hasConflict && state.value.conflictReady) {
                mutableState.update { it.copy(name = it.snapshot?.profile?.name.orEmpty(), emoji = it.snapshot?.profile?.emoji,
                    hasConflict = false, conflictReady = false, operationError = null, profileSubmitted = false) }
            }
            TogetherUiIntent.SaveDraftOverLatest -> if (state.value.hasConflict && state.value.conflictReady) {
                mutableState.update { it.copy(hasConflict = false, conflictReady = false, operationError = null) }
                continueProfile()
            }
            else -> mutableState.update { current ->
                when (intent) {
                    is TogetherUiIntent.ChooseRole -> if (current.step == TogetherStep.Start && current.snapshot != null && !current.savedAwaitingRefresh) {
                        current.copy(step = TogetherStep.Profile, role = intent.role, codeInput = "", invitation = null, checked = null, invitationFailure = null, acceptanceUncertain = false,
                            operationError = null, profileSubmitted = false)
                    } else current
                    TogetherUiIntent.EditProfile -> if (current.snapshot != null && !current.savedAwaitingRefresh) {
                        current.copy(step = TogetherStep.Profile, role = null, operationError = null, profileSubmitted = false)
                    } else current
                    is TogetherUiIntent.ChangeName -> if (current.step == TogetherStep.Profile) current.copy(name = intent.value) else current
                    is TogetherUiIntent.SelectEmoji -> if (current.step == TogetherStep.Profile) current.copy(emoji = intent.emoji) else current
                    is TogetherUiIntent.ChangeCode -> if (current.step == TogetherStep.Receive) current.copy(codeInput = intent.value, codeSubmitted = false, checked = null, invitation = null, invitationFailure = null, operationError = null, acceptanceUncertain = false) else current
                }
            }
        }
    }

    /** 발급 응답이 불확실해도 자동 재발급하지 않는다. 명시적인 재발급마다 새 요청 ID를 사용한다. */
    private fun issueInvitation() {
        val requestId = Uuid.random().toString()
        mutableState.update { it.copy(isSaving = true, issued = null, reissueConfirmation = false,
            issuanceUncertain = true, invitationFailure = null, operationError = null, expired = false) }
        work = viewModelScope.launch {
            when (val result = repository.issueInvitation(requestId)) {
                is AppResult.Success -> mutableState.update { it.copy(isSaving = false, issued = result.data, issuanceUncertain = false,
                    snapshot = it.snapshot?.copy(invitation = PendingInvitation(result.data.id, result.data.expiresAt, false))) }
                is AppResult.Failure -> invitationFailed(result.error)
            }
        }
    }

    private fun checkInvitation() {
        val current = state.value
        if (current.step != TogetherStep.Receive) return
        val code = InvitationCode.parse(current.codeInput)
        mutableState.update { it.copy(codeSubmitted = true, checked = null, invitation = null, invitationFailure = null, operationError = null) }
        if (code == null) return
        mutableState.update { it.copy(isLoading = true) }
        val version = ++generation
        work = viewModelScope.launch {
            val result = repository.previewInvitation(code)
            if (version != generation) return@launch
            when (result) {
                is AppResult.Success -> mutableState.update { it.copy(isLoading = false, checked = result.data,
                    invitation = InvitationDisplay(result.data.inviterName, result.data.inviterEmoji, InvitationStatus.Available),
                    expired = Clock.System.now() >= result.data.expiresAt, step = TogetherStep.Confirmation) }
                is AppResult.Failure -> invitationFailed(result.error)
            }
        }
    }

    private fun acceptInvitation() {
        val current = state.value
        if (current.step != TogetherStep.Confirmation || current.checked == null) return
        if (Clock.System.now() >= current.checked.expiresAt) { onIntent(TogetherUiIntent.Tick); return }
        val code = InvitationCode.parse(current.codeInput) ?: return
        mutableState.update { it.copy(isSaving = true, acceptanceUncertain = true, operationError = null, invitationFailure = null) }
        work = viewModelScope.launch {
            when (val result = repository.acceptInvitation(code)) {
                is AppResult.Success -> mutableState.update {
                    val own = result.data.members.singleOrNull { member -> member.userId == it.snapshot?.profile?.userId }
                        ?: return@update it.copy(isSaving = false, operationError = TogetherOperationError.InvalidResponse)
                    it.copy(isSaving = false, acceptanceUncertain = false, issuanceUncertain = false, issued = null, checked = null,
                        invitation = null, codeInput = "", step = TogetherStep.Connected,
                        snapshot = TogetherSnapshot(own, result.data, null))
                }
                is AppResult.Failure -> invitationFailed(result.error)
            }
        }
    }

    private fun invitationFailed(error: AppError) {
        mutableState.update { it.copy(isSaving = false, isLoading = false,
            invitationFailure = (error as? AppError.Invitation)?.reason,
            operationError = if (error is AppError.Invitation) null else error.toUi(),
            acceptanceUncertain = it.acceptanceUncertain && error is AppError.Remote,
            issuanceUncertain = it.issuanceUncertain && error is AppError.Remote,
            step = if (error == AppError.Invitation(InvitationFailure.ProfileRequired)) TogetherStep.Profile else it.step) }
    }

    private fun refresh() {
        val initial = state.value
        val preserveDraft = initial.step == TogetherStep.Profile
        val afterSave = initial.savedAwaitingRefresh
        val version = ++generation
        work?.cancel()
        mutableState.update { it.copy(isLoading = true, operationError = null) }
        work = viewModelScope.launch {
            when (val result = repository.load()) {
                is AppResult.Success -> if (version == generation) {
                    mutableState.update { current ->
                        applySnapshot(current, result.data, preserveDraft, afterSave).let {
                            if (current.hasConflict || (preserveDraft && !afterSave && current.snapshot?.profile != result.data.profile &&
                                (result.data.profile?.name != ProfileName.normalize(current.name) || result.data.profile?.emoji != current.emoji))) it.copy(hasConflict = true, conflictReady = true, operationError = TogetherOperationError.Conflict)
                            else it
                        }
                    }
                    if (afterSave && state.value.step == TogetherStep.Receive && InvitationCode.parse(state.value.codeInput) != null) checkInvitation()
                }
                is AppResult.Failure -> if (version == generation) {
                    mutableState.update { it.copy(isLoading = false, operationError = result.error.toUi()) }
                }
            }
        }
    }

    private fun continueProfile() {
        val current = state.value
        if (current.step != TogetherStep.Profile || current.hasConflict || current.savedAwaitingRefresh || current.snapshot == null) return
        if (!ProfileName.isValid(current.name) || current.emoji == null) {
            mutableState.update { it.copy(profileSubmitted = true) }
            return
        }
        val version = ++generation
        work?.cancel()
        mutableState.update { it.copy(isSaving = true, operationError = null, profileSubmitted = false) }
        work = viewModelScope.launch {
            when (val saved = repository.saveProfile(current.name, current.emoji, current.snapshot.profile?.revision)) {
                is AppResult.Failure -> if (version == generation) {
                    mutableState.update { it.copy(isSaving = false, operationError = saved.error.toUi(),
                        hasConflict = saved.error == AppError.Conflict, conflictReady = false) }
                    // 수정 충돌은 먼저 최신 값을 조회하되 입력 초안은 유지한다.
                    if (saved.error == AppError.Conflict) refresh()
                }
                is AppResult.Success -> if (version == generation) {
                    mutableState.update { value ->
                        val previous = value.snapshot ?: return@update value
                        value.copy(isSaving = false, isLoading = true, savedAwaitingRefresh = true,
                            name = saved.data.name, emoji = saved.data.emoji,
                            snapshot = previous.copy(profile = saved.data,
                                household = previous.household?.let { home -> home.copy(members = home.members.map {
                                    if (it.userId == saved.data.userId) saved.data else it
                                }) }))
                    }
                    when (val loaded = repository.load()) {
                        is AppResult.Success -> if (version == generation) {
                            mutableState.update { applySnapshot(it, loaded.data, false, true) }
                            if (state.value.step == TogetherStep.Receive && InvitationCode.parse(state.value.codeInput) != null) checkInvitation()
                        }
                        is AppResult.Failure -> if (version == generation) mutableState.update {
                            it.copy(isLoading = false, operationError = loaded.error.toUi())
                        }
                    }
                }
            }
        }
    }

    private fun applySnapshot(current: TogetherUiState, snapshot: TogetherSnapshot, preserveDraft: Boolean, afterSave: Boolean): TogetherUiState {
        val next = when {
            preserveDraft && current.hasConflict -> TogetherStep.Profile
            snapshot.connected && (!preserveDraft || afterSave) -> TogetherStep.Connected
            afterSave -> when (current.role) {
                TogetherRole.Inviter -> TogetherStep.Invite
                TogetherRole.Invitee -> TogetherStep.Receive
                null -> if (snapshot.connected) TogetherStep.Connected else TogetherStep.Start
            }
            current.step == TogetherStep.Connected && !snapshot.connected -> TogetherStep.Start
            else -> current.step
        }
        val issued = current.issued?.takeIf { !snapshot.connected && snapshot.invitation?.id == it.id && snapshot.invitation?.expired == false }
        return current.copy(issued = issued, checked = if (snapshot.connected) null else current.checked,
            invitation = if (snapshot.connected) null else current.invitation,
            acceptanceUncertain = if (snapshot.connected) false else current.acceptanceUncertain,
            snapshot = snapshot, isLoading = false, savedAwaitingRefresh = false,
            name = if (preserveDraft) current.name else snapshot.profile?.name.orEmpty(),
            emoji = if (preserveDraft) current.emoji else snapshot.profile?.emoji,
            step = next, operationError = null)
    }
}

private fun AppError.toUi(): TogetherOperationError = when (this) {
    AppError.Conflict -> TogetherOperationError.Conflict
    AppError.InvalidInput -> TogetherOperationError.InvalidInput
    AppError.Storage -> TogetherOperationError.Storage
    AppError.NotFound -> TogetherOperationError.Unavailable
    is AppError.Invitation -> TogetherOperationError.Unavailable
    is AppError.Remote -> when (reason) {
        RemoteFailure.Configuration -> TogetherOperationError.Configuration
        RemoteFailure.Network -> TogetherOperationError.Network
        RemoteFailure.Authentication -> TogetherOperationError.Authentication
        RemoteFailure.Permission -> TogetherOperationError.Permission
        RemoteFailure.RateLimited -> TogetherOperationError.RateLimited
        RemoteFailure.Unavailable -> TogetherOperationError.Unavailable
        RemoteFailure.InvalidResponse -> TogetherOperationError.InvalidResponse
    }
}
