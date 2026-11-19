package com.woorisseum.feature.shopping.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.component.SwipeToRevealDeleteRow
import com.woorisseum.core.domain.shopping.ShoppingItem
import com.woorisseum.feature.shopping.model.*
import com.woorisseum.feature.shopping.resources.*
import org.jetbrains.compose.resources.stringResource

/** 목록은 표시와 사용자 의도 전달만 담당한다. ID 기반 수정·그룹 전이는 ViewModel이 소유한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    state: ShoppingListUiState,
    onIntent: (ShoppingListUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var revealedItemId by remember { mutableStateOf<String?>(null) }
    val onRevealItem: (String?) -> Unit = { itemId ->
        revealedItemId = itemId
        if (itemId != null) {
            focus.clearFocus()
            keyboard?.hide()
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress, state.editor) {
        if (listState.isScrollInProgress || state.editor != null) revealedItemId = null
    }
    val pending = state.pendingItems
    val purchased = state.purchasedItems
    Scaffold(
        modifier = modifier.imePadding(), containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.shopping_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(Res.string.shopping_back)) } },
            actions = { TextButton(onClick = onOpenHistory) {
                Text(stringResource(Res.string.shopping_history_count, state.trips.size))
            } }) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShoppingCategorySelector(state.quickCategory) { onIntent(ShoppingListUiIntent.QuickCategoryChanged(it)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.quickNameInput,
                            onValueChange = { onIntent(ShoppingListUiIntent.QuickNameChanged(it)) },
                            label = { Text(stringResource(Res.string.shopping_quick_name)) },
                            isError = state.quickNameError != null,
                            singleLine = true, modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) revealedItemId = null },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onIntent(ShoppingListUiIntent.AddItem) }),
                        )
                        Button(onClick = { onIntent(ShoppingListUiIntent.AddItem) }) {
                            Text(stringResource(Res.string.shopping_add))
                        }
                    }
                    state.quickNameError?.let { Text(shoppingNameErrorText(it),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    if (state.duplicateItemId != null) {
                        TextButton(onClick = {
                            focus.clearFocus(); keyboard?.hide()
                            onIntent(ShoppingListUiIntent.OpenDuplicateItem)
                        }) { Text(stringResource(Res.string.shopping_open_existing)) }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item(key = "notice") {
                Text(stringResource(Res.string.shopping_temporary_notice), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item(key = "presence") { ShoppingPresenceHeader(state.presence) }
            item(key = "finish") {
                OutlinedButton(onClick = {
                    revealedItemId = null
                    focus.clearFocus(); keyboard?.hide()
                    onIntent(ShoppingListUiIntent.RequestFinish)
                }, enabled = state.totalPurchasedCount > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.shopping_finish_count, state.totalPurchasedCount))
                }
            }
            item(key = "category-filter") {
                ShoppingCategoryFilters(state.categoryFilter) {
                    revealedItemId = null
                    onIntent(ShoppingListUiIntent.CategoryFilterChanged(it))
                }
            }
            item(key = "pending-header") {
                Text(stringResource(Res.string.shopping_pending_count, pending.size),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (pending.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(when {
                            purchased.isNotEmpty() -> Res.string.shopping_all_purchased
                            state.categoryFilter != ShoppingCategoryFilter.All -> Res.string.shopping_category_empty
                            else -> Res.string.shopping_empty_title
                        }))
                        if (purchased.isEmpty() && state.categoryFilter == ShoppingCategoryFilter.All) Text(stringResource(Res.string.shopping_empty_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(pending, key = { it.id }) { item -> ShoppingItemRow(item, state.presence, onIntent,
                    revealed = revealedItemId == item.id, onReveal = onRevealItem) }
            item(key = "purchased-header") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(onClick = { revealedItemId = null; onIntent(ShoppingListUiIntent.TogglePurchasedSection) }, enabled = purchased.isNotEmpty()) {
                    Text(stringResource(Res.string.shopping_purchased_count, purchased.size))
                    if (purchased.isNotEmpty()) Text(" · " + stringResource(if (state.purchasedExpanded)
                        Res.string.shopping_collapse else Res.string.shopping_expand))
                }
            }
            if (state.purchasedExpanded) {
                items(purchased, key = { it.id }) { item -> ShoppingItemRow(item, state.presence, onIntent,
                    revealed = revealedItemId == item.id, onReveal = onRevealItem) }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(item: ShoppingItem, presence: ShoppingPresenceUiState,
    onIntent: (ShoppingListUiIntent) -> Unit, revealed: Boolean, onReveal: (String?) -> Unit) {
    val live = presence as? ShoppingPresenceUiState.Connected
    val editing = live?.editing?.get(item.id).orEmpty()
    val recent = live?.recentChanges?.get(item.id)
    val checkLabel = stringResource(if (item.purchased) Res.string.shopping_uncomplete_item
        else Res.string.shopping_complete_item, item.name)
    val deleteLabel = stringResource(Res.string.shopping_delete_item, item.name)
    SwipeToRevealDeleteRow(item.id, revealed, onReveal, deleteLabel, stringResource(Res.string.shopping_delete),
        onDelete = { onReveal(null); onIntent(ShoppingListUiIntent.DeleteItem(item.id)) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = item.purchased,
                onCheckedChange = { onReveal(null); onIntent(ShoppingListUiIntent.SetPurchased(item.id, it)) },
                modifier = Modifier.semantics { contentDescription = checkLabel })
            val editLabel = stringResource(Res.string.shopping_edit_item, item.name)
            Column(Modifier.weight(1f).background(if (recent != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                else Color.Transparent).clickable(role = Role.Button, onClickLabel = editLabel,
                onClick = { if (revealed) onReveal(null) else { onReveal(null); onIntent(ShoppingListUiIntent.OpenEditor(item.id)) } }).heightIn(min = 48.dp).padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(item.name, Modifier.weight(1f), textDecoration = if (item.purchased) TextDecoration.LineThrough else null,
                        color = if (item.purchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    item.quantity?.let { Text(it, Modifier.widthIn(max = 120.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                item.category?.let { Text(shoppingCategoryLabel(it),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
                item.memo?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                when {
                    editing.isNotEmpty() -> Text(stringResource(Res.string.shopping_editing, editing.joinToString(", ") { it.displayName }),
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    recent != null -> Text(stringResource(Res.string.shopping_recent_change, recent.displayName),
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
internal fun ShoppingPresenceHeader(presence: ShoppingPresenceUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (presence) {
            ShoppingPresenceUiState.NotConnected -> {
                Text(stringResource(Res.string.shopping_disconnected), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(Res.string.shopping_presence_unavailable), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is ShoppingPresenceUiState.Connected -> Text(
                if (presence.viewers.isEmpty()) stringResource(Res.string.shopping_no_viewers)
                else stringResource(Res.string.shopping_viewers, presence.viewers.joinToString(", ") { it.displayName }),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun shoppingNameErrorText(error: ShoppingNameError): String = stringResource(when (error) {
    ShoppingNameError.REQUIRED -> Res.string.shopping_name_required
    ShoppingNameError.DUPLICATE -> Res.string.shopping_name_duplicate
})
