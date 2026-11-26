package com.woorisseum.feature.household.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.woorisseum.core.domain.household.InvitationCode
import com.woorisseum.core.domain.common.InvitationFailure
import androidx.compose.runtime.key
import com.woorisseum.core.domain.household.ProfileName
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.component.*
import com.woorisseum.feature.household.model.*
import com.woorisseum.feature.household.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TogetherScreen(
    state: TogetherUiState,
    onIntent: (TogetherUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    codeCopied: Boolean = false,
    onShareCode: (String) -> Unit = {},
    onCopyCode: (String) -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val busy = state.isSaving || state.isLoading
    if (state.reissueConfirmation) AlertDialog(
        onDismissRequest = { onIntent(TogetherUiIntent.CancelReissue) },
        title = { Text(stringResource(Res.string.together_reissue)) },
        text = { Text(stringResource(Res.string.together_reissue_warning)) },
        confirmButton = { WoorisseumTextButton(onClick = { onIntent(TogetherUiIntent.ConfirmReissue) }) { Text(stringResource(Res.string.together_reissue)) } },
        dismissButton = { WoorisseumTextButton(onClick = { onIntent(TogetherUiIntent.CancelReissue) }) { Text(stringResource(Res.string.together_cancel)) } },
    )
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            WoorisseumTopAppBar(title = { Text(stringResource(Res.string.together_title)) },
                navigationIcon = {
                    WoorisseumTextButton(onClick = onBack) { Text(stringResource(Res.string.together_back)) }
                })
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(key(state.step) { rememberScrollState() })
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.isLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.together_loading))
                }
            }
            state.operationError?.let { error ->
                if (!(state.hasConflict && error == TogetherOperationError.Conflict)) ErrorText(error.text())
                if (state.savedAwaitingRefresh) Text(stringResource(Res.string.together_saved_refresh_failed))
                if (!state.isLoading && !state.isSaving) {
                    WoorisseumTextButton(onClick = { onIntent(TogetherUiIntent.Refresh) }) {
                        Text(stringResource(Res.string.together_refresh))
                    }
                }
            }
            state.invitationFailure?.let {
                ErrorText(it.text())
                if (!busy) WoorisseumTextButton(onClick = { onIntent(TogetherUiIntent.Refresh) }) {
                    Text(stringResource(Res.string.together_refresh))
                }
            }
            if (state.issuanceUncertain && !busy) Notice(Res.string.together_issue_uncertain)
            if (state.acceptanceUncertain && !busy) Notice(Res.string.together_accept_uncertain)
            when (state.step) {
                TogetherStep.Start -> {
                    Heading(Res.string.together_start_title, Res.string.together_start_body)
                    state.snapshot?.profile?.let {
                        Person(it.name, it.emoji)
                        WoorisseumTextButton(onClick = { onIntent(TogetherUiIntent.EditProfile) }, enabled = !state.isLoading) {
                            Text(stringResource(Res.string.together_edit_profile))
                        }
                    }
                    if (state.snapshot?.household != null) Notice(Res.string.together_waiting)
                    Action(Res.string.together_invite, enabled = state.snapshot != null && !state.isLoading && !state.savedAwaitingRefresh) { onIntent(TogetherUiIntent.ChooseRole(TogetherRole.Inviter)) }
                    WoorisseumSecondaryButton(
                        onClick = { onIntent(TogetherUiIntent.ChooseRole(TogetherRole.Invitee)) },
                        modifier = Modifier.fillMaxWidth(), enabled = state.snapshot != null && !state.isLoading && !state.savedAwaitingRefresh,
                    ) { Text(stringResource(Res.string.together_receive)) }
                    if (state.operationError == null) WoorisseumTextButton(onClick = { onIntent(TogetherUiIntent.Refresh) }, enabled = !state.isLoading) {
                        Text(stringResource(Res.string.together_refresh))
                    }
                }
                TogetherStep.Profile -> {
                    Heading(Res.string.together_profile_title, Res.string.together_profile_body)
                    WoorisseumTextField(
                        value = state.name, onValueChange = { onIntent(TogetherUiIntent.ChangeName(it)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !(state.isSaving || state.isLoading),
                        label = { Text(stringResource(Res.string.together_name)) },
                        isError = state.profileSubmitted && !ProfileName.isValid(state.name),
                        supportingText = {
                            if (state.profileSubmitted && !ProfileName.isValid(state.name)) ErrorText(Res.string.together_name_error)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    )
                    Text(stringResource(Res.string.together_emoji_title), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TogetherEmoji.entries.forEach { emoji ->
                            val label = stringResource(emoji.label())
                            WoorisseumFilterChip(
                                selected = emoji == state.emoji, enabled = !(state.isSaving || state.isLoading),
                                onClick = { focus.clearFocus(); onIntent(TogetherUiIntent.SelectEmoji(emoji)) },
                                modifier = Modifier.semantics { contentDescription = label },
                                label = { Text(stringResource(emoji.glyph()), style = MaterialTheme.typography.headlineSmall) },
                            )
                        }
                    }
                    if (state.profileSubmitted && state.emoji == null) ErrorText(Res.string.together_emoji_error)
                    if (state.hasConflict) {
                        ErrorText(Res.string.together_profile_conflict)
                        if (state.conflictReady) {
                            Text(stringResource(Res.string.together_latest_profile), style = MaterialTheme.typography.titleMedium)
                            state.snapshot?.profile?.let { Person(it.name, it.emoji) }
                                ?: Text(stringResource(Res.string.together_profile_missing))
                            WoorisseumSecondaryButton(onClick = { onIntent(TogetherUiIntent.UseServerProfile) }, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(Res.string.together_use_latest))
                            }
                            Action(Res.string.together_keep_draft, enabled = !state.isLoading) { onIntent(TogetherUiIntent.SaveDraftOverLatest) }
                        }
                    }
                    WoorisseumPrimaryButton(
                        onClick = { focus.clearFocus(); onIntent(TogetherUiIntent.ContinueProfile) },
                        enabled = !(state.isSaving || state.isLoading) && !state.hasConflict && !state.savedAwaitingRefresh, modifier = Modifier.fillMaxWidth(),
                    ) {
                        if ((state.isSaving || state.isLoading)) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(stringResource(Res.string.together_next))
                    }
                }
                TogetherStep.Invite -> {
                    Heading(Res.string.together_invite_title, Res.string.together_invite_body)
                    Person(state.name, state.emoji)
                    state.issued?.let { issued ->
                        Text(issued.code.value, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(Res.string.together_expires_at, issued.expiresAt.toString()), style = MaterialTheme.typography.bodySmall)
                        if (state.expired) Notice(Res.string.together_expired)
                        Action(Res.string.together_share, enabled = !busy && !state.expired) { onShareCode(issued.code.value) }
                        WoorisseumSecondaryButton(onClick = { onCopyCode(issued.code.value) }, enabled = !busy && !state.expired, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (codeCopied) Res.string.together_copied else Res.string.together_copy))
                        }
                    } ?: Text(stringResource(Res.string.together_code_missing), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Action(if (state.issued != null || state.snapshot?.invitation != null || state.issuanceUncertain) Res.string.together_reissue else Res.string.together_create, enabled = !busy) { onIntent(TogetherUiIntent.RequestInvite) }
                    WoorisseumTextButton(onClick = { onIntent(TogetherUiIntent.Refresh) }, enabled = !busy) {
                        Text(stringResource(Res.string.together_refresh))
                    }
                }
                TogetherStep.Receive -> {
                    Heading(Res.string.together_receive_title, Res.string.together_receive_body)
                    Person(state.name, state.emoji)
                    WoorisseumTextField(
                        value = state.codeInput, onValueChange = { onIntent(TogetherUiIntent.ChangeCode(it)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        label = { Text(stringResource(Res.string.together_link)) },
                        isError = state.codeSubmitted && InvitationCode.parse(state.codeInput) == null,
                        supportingText = {
                            if (state.codeSubmitted && InvitationCode.parse(state.codeInput) == null) ErrorText(Res.string.together_link_error)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); onIntent(TogetherUiIntent.CheckInvitation) }),
                    )
                    Action(Res.string.together_check, enabled = !busy) { focus.clearFocus(); onIntent(TogetherUiIntent.CheckInvitation) }

                }
                TogetherStep.Confirmation -> {
                    Text(stringResource(Res.string.together_confirmation_title), style = MaterialTheme.typography.headlineSmall)
                    state.invitation?.let { invitation ->
                        Person(stringResource(Res.string.together_inviter, invitation.inviterName), invitation.inviterEmoji)
                        when (invitation.status) {
                            InvitationStatus.Available -> {
                                Person(state.name, state.emoji)
                                if (state.expired) Notice(Res.string.together_expired)
                                Action(Res.string.together_accept, enabled = !busy && !state.expired && state.invitationFailure == null) { onIntent(TogetherUiIntent.AcceptInvitation) }
                            }
                            InvitationStatus.Expired -> Notice(Res.string.together_expired)
                            InvitationStatus.Used -> Notice(Res.string.together_used)
                            InvitationStatus.Full -> Notice(Res.string.together_full)
                        }
                    }
                    WoorisseumSecondaryButton(onClick = { onIntent(TogetherUiIntent.Back) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.together_edit_link))
                    }
                }
                TogetherStep.Connected -> {
                    Heading(Res.string.together_connected_title, Res.string.together_connected_body)
                    Text(stringResource(Res.string.together_two_people), style = MaterialTheme.typography.titleMedium)
                    state.snapshot?.household?.members?.forEach { Person(it.name, it.emoji) }
                    WoorisseumSecondaryButton(onClick = { onIntent(TogetherUiIntent.EditProfile) }, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.together_edit_profile))
                    }
                    if (state.operationError == null) WoorisseumTextButton(onClick = { onIntent(TogetherUiIntent.Refresh) }, enabled = !state.isLoading) {
                        Text(stringResource(Res.string.together_refresh))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(Res.string.together_local_notice), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Heading(title: StringResource, body: StringResource) {
    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Person(name: String, emoji: TogetherEmoji?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        emoji?.let { Text(stringResource(it.glyph()), style = MaterialTheme.typography.headlineMedium) }
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Notice(text: StringResource) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Text(stringResource(text), Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorText(text: StringResource) {
    Text(stringResource(text), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun Action(text: StringResource, enabled: Boolean = true, onClick: () -> Unit) {
    WoorisseumPrimaryButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(stringResource(text)) }
}

private fun TogetherEmoji.glyph(): StringResource = when (this) {
    TogetherEmoji.Cat -> Res.string.emoji_cat
    TogetherEmoji.Dog -> Res.string.emoji_dog
    TogetherEmoji.Rabbit -> Res.string.emoji_rabbit
    TogetherEmoji.Bear -> Res.string.emoji_bear
    TogetherEmoji.Fox -> Res.string.emoji_fox
    TogetherEmoji.Panda -> Res.string.emoji_panda
    TogetherEmoji.Frog -> Res.string.emoji_frog
    TogetherEmoji.Penguin -> Res.string.emoji_penguin
}

private fun TogetherEmoji.label(): StringResource = when (this) {
    TogetherEmoji.Cat -> Res.string.emoji_cat_label
    TogetherEmoji.Dog -> Res.string.emoji_dog_label
    TogetherEmoji.Rabbit -> Res.string.emoji_rabbit_label
    TogetherEmoji.Bear -> Res.string.emoji_bear_label
    TogetherEmoji.Fox -> Res.string.emoji_fox_label
    TogetherEmoji.Panda -> Res.string.emoji_panda_label
    TogetherEmoji.Frog -> Res.string.emoji_frog_label
    TogetherEmoji.Penguin -> Res.string.emoji_penguin_label
}

private fun TogetherOperationError.text(): StringResource = when (this) {
    TogetherOperationError.Configuration -> Res.string.together_config_error
    TogetherOperationError.Network -> Res.string.together_network_error
    TogetherOperationError.Authentication -> Res.string.together_auth_error
    TogetherOperationError.Permission -> Res.string.together_permission_error
    TogetherOperationError.RateLimited -> Res.string.together_rate_limit_error
    TogetherOperationError.InvalidResponse -> Res.string.together_response_error
    TogetherOperationError.Conflict -> Res.string.together_profile_conflict
    TogetherOperationError.InvalidInput -> Res.string.together_profile_invalid
    TogetherOperationError.Storage -> Res.string.together_storage_error
    TogetherOperationError.Unavailable -> Res.string.together_server_error
}

private fun InvitationFailure.text(): StringResource = when (this) {
    InvitationFailure.ProfileRequired -> Res.string.together_profile_missing
    InvitationFailure.NotFound -> Res.string.together_invitation_missing
    InvitationFailure.Self -> Res.string.together_invitation_self
    InvitationFailure.Used -> Res.string.together_used
    InvitationFailure.Revoked -> Res.string.together_invitation_revoked
    InvitationFailure.Expired -> Res.string.together_expired
    InvitationFailure.Full -> Res.string.together_full
    InvitationFailure.AlreadyConnected -> Res.string.together_already_connected
    InvitationFailure.CurrentHouseholdHasData -> Res.string.together_space_has_data
    InvitationFailure.IssuanceResultUnavailable -> Res.string.together_issue_uncertain
}
