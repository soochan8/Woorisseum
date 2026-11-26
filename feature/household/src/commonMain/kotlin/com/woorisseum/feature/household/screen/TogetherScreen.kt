package com.woorisseum.feature.household.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
) {
    val focus = LocalFocusManager.current
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
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when (state.step) {
                TogetherStep.Start -> {
                    Heading(Res.string.together_start_title, Res.string.together_start_body)
                    Action(Res.string.together_invite) { onIntent(TogetherUiIntent.ChooseRole(TogetherRole.Inviter)) }
                    WoorisseumSecondaryButton(
                        onClick = { onIntent(TogetherUiIntent.ChooseRole(TogetherRole.Invitee)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(Res.string.together_receive)) }
                }
                TogetherStep.Profile -> {
                    Heading(Res.string.together_profile_title, Res.string.together_profile_body)
                    WoorisseumTextField(
                        value = state.name, onValueChange = { onIntent(TogetherUiIntent.ChangeName(it)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.isAuthenticating,
                        label = { Text(stringResource(Res.string.together_name)) },
                        isError = state.profileSubmitted && state.name.isBlank(),
                        supportingText = {
                            if (state.profileSubmitted && state.name.isBlank()) ErrorText(Res.string.together_name_error)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    )
                    Text(stringResource(Res.string.together_emoji_title), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TogetherEmoji.entries.forEach { emoji ->
                            val label = stringResource(emoji.label())
                            WoorisseumFilterChip(
                                selected = emoji == state.emoji, enabled = !state.isAuthenticating,
                                onClick = { focus.clearFocus(); onIntent(TogetherUiIntent.SelectEmoji(emoji)) },
                                modifier = Modifier.semantics { contentDescription = label },
                                label = { Text(stringResource(emoji.glyph()), style = MaterialTheme.typography.headlineSmall) },
                            )
                        }
                    }
                    if (state.profileSubmitted && state.emoji == null) ErrorText(Res.string.together_emoji_error)
                    state.sessionError?.let {
                        ErrorText(if (it == SessionError.Configuration) Res.string.together_config_error else Res.string.together_session_error)
                    }
                    WoorisseumPrimaryButton(
                        onClick = { focus.clearFocus(); onIntent(TogetherUiIntent.ContinueProfile) },
                        enabled = !state.isAuthenticating, modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isAuthenticating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(stringResource(Res.string.together_next))
                    }
                }
                TogetherStep.Invite -> {
                    Heading(Res.string.together_invite_title, Res.string.together_invite_body)
                    Person(state.name, state.emoji)
                    Notice(if (state.showPreparation) Res.string.together_preparation_result else Res.string.together_not_ready)
                    Text(stringResource(Res.string.together_no_link), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Action(Res.string.together_create) { onIntent(TogetherUiIntent.RequestInvite) }
                    WoorisseumSecondaryButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.together_share))
                    }
                }
                TogetherStep.Receive -> {
                    Heading(Res.string.together_receive_title, Res.string.together_receive_body)
                    Person(state.name, state.emoji)
                    WoorisseumTextField(
                        value = state.link, onValueChange = { onIntent(TogetherUiIntent.ChangeLink(it)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(stringResource(Res.string.together_link)) },
                        isError = state.linkSubmitted && state.link.isBlank(),
                        supportingText = {
                            if (state.linkSubmitted && state.link.isBlank()) ErrorText(Res.string.together_link_error)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); onIntent(TogetherUiIntent.CheckInvitation) }),
                    )
                    Action(Res.string.together_check) { focus.clearFocus(); onIntent(TogetherUiIntent.CheckInvitation) }
                    Notice(if (state.showPreparation) Res.string.together_preparation_result else Res.string.together_not_ready)
                }
                TogetherStep.Confirmation -> {
                    Text(stringResource(Res.string.together_confirmation_title), style = MaterialTheme.typography.headlineSmall)
                    state.invitation?.let { invitation ->
                        Person(stringResource(Res.string.together_inviter, invitation.inviterName), invitation.inviterEmoji)
                        when (invitation.status) {
                            InvitationStatus.Available -> {
                                Person(state.name, state.emoji)
                                Action(Res.string.together_accept) { onIntent(TogetherUiIntent.AcceptInvitation) }
                            }
                            InvitationStatus.Expired -> Notice(Res.string.together_expired)
                            InvitationStatus.Used -> Notice(Res.string.together_used)
                            InvitationStatus.Full -> Notice(Res.string.together_full)
                        }
                    }
                    if (state.showPreparation) Notice(Res.string.together_preparation_result)
                    WoorisseumSecondaryButton(onClick = { onIntent(TogetherUiIntent.Back) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.together_edit_link))
                    }
                }
                TogetherStep.Connected -> {
                    Heading(Res.string.together_connected_title, Res.string.together_connected_body)
                    Text(stringResource(Res.string.together_two_people), style = MaterialTheme.typography.titleMedium)
                    Person(state.name, state.emoji)
                    state.invitation?.let { Person(it.inviterName, it.inviterEmoji) }
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
private fun Action(text: StringResource, onClick: () -> Unit) {
    WoorisseumPrimaryButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(stringResource(text)) }
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
