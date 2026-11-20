package com.woorisseum.feature.transaction.card

import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.component.WoorisseumTextField
import com.woorisseum.core.designsystem.component.WoorisseumBottomActionBar
import com.woorisseum.core.designsystem.component.WoorisseumPrimaryButton
import com.woorisseum.feature.transaction.resources.*
import com.woorisseum.core.designsystem.component.SwipeToRevealDeleteRow
import org.jetbrains.compose.resources.stringResource

@Composable
fun CardListScreen(
    state: CardListUiState,
    onIntent: (CardListUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealedCardId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) revealedCardId = null
    }
    CardScaffold(stringResource(Res.string.card_management_title), onBack,
        stringResource(Res.string.card_register_title), { onIntent(CardListUiIntent.AddCard) },
        modifier = modifier, enabled = state.deletingCardId == null) { padding ->
        if (state.cards.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.card_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.card_empty_description),
                    modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(24.dp)) {
                if (state.deleteFailed) item(key = "delete-error") {
                    Text(stringResource(Res.string.card_delete_failed), color = MaterialTheme.colorScheme.error)
                }
                items(state.cards, key = { it.id }) { card ->
                    SwipeToRevealDeleteRow(
                        itemId = card.id,
                        revealed = revealedCardId == card.id,
                        onReveal = { revealedCardId = it },
                        deleteLabel = stringResource(Res.string.card_delete_item, card.alias, card.issuer),
                        deleteText = stringResource(Res.string.card_delete),
                        onDelete = {
                            revealedCardId = null
                            onIntent(CardListUiIntent.DeleteCard(card.id))
                        },
                        enabled = state.deletingCardId == null,
                    ) {
                        ListItem(
                            headlineContent = { Text(card.alias) },
                            supportingContent = { Text(card.issuer) },
                            trailingContent = { Text(stringResource(Res.string.card_edit_action)) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.clickable(enabled = state.deletingCardId == null) {
                                if (revealedCardId == card.id) revealedCardId = null else {
                                    revealedCardId = null
                                    onIntent(CardListUiIntent.EditCard(card.id))
                                }
                            },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun CardEditorScreen(
    state: CardEditorUiState,
    onIntent: (CardEditorUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    CardScaffold(
        stringResource(if (state.cardId == null) Res.string.card_register_title else Res.string.card_edit_title),
        onBack, stringResource(Res.string.expense_save), {
            focus.clearFocus()
            keyboard?.hide()
            onIntent(CardEditorUiIntent.Save)
        }, enabled = !state.isLoading && !state.notFound && !state.isSaving && !state.saved,
        modifier = modifier,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.notFound -> Text(stringResource(Res.string.card_not_found))
                else -> {
                    CardTextField(state.alias, { onIntent(CardEditorUiIntent.AliasChanged(it)) },
                        stringResource(Res.string.card_alias_label), stringResource(Res.string.card_alias_example),
                        state.aliasRequired, !state.isSaving && !state.saved, ImeAction.Next)
                    CardTextField(state.issuer, { onIntent(CardEditorUiIntent.IssuerChanged(it)) },
                        stringResource(Res.string.card_issuer_label), stringResource(Res.string.card_issuer_example),
                        state.issuerRequired, !state.isSaving && !state.saved, ImeAction.Done)
                    if (state.saveFailed) {
                        Text(stringResource(Res.string.card_save_failed), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/** 카드 등록·수정에서 같은 필드 표현과 필수값 오류 안내를 사용한다. */
@Composable
private fun CardTextField(
    value: String, onValueChange: (String) -> Unit, label: String, example: String,
    required: Boolean, enabled: Boolean, imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    WoorisseumTextField(value, onValueChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = { Text(example) }, singleLine = true,
        enabled = enabled, isError = required, keyboardOptions = KeyboardOptions(imeAction = imeAction),
        supportingText = if (required) { { Text(stringResource(Res.string.card_field_required)) } } else null)
}

/** 카드 화면의 상단 이동·하단 실행 버튼과 시스템 영역 처리를 공유한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardScaffold(
    title: String, onBack: () -> Unit, action: String, onAction: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(modifier = modifier.imePadding(), containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = { Text(title, style = MaterialTheme.typography.headlineSmall) }, navigationIcon = {
                WoorisseumTextButton(onClick = onBack) { Text(stringResource(Res.string.expense_back)) }
            })
        }, bottomBar = {
            WoorisseumBottomActionBar {
                WoorisseumPrimaryButton(onClick = onAction, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(action) }
            }
        }, content = content)
}
