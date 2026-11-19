package com.woorisseum.feature.shopping.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.woorisseum.feature.shopping.model.*
import com.woorisseum.feature.shopping.resources.*
import org.jetbrains.compose.resources.stringResource

/** 원본을 직접 바꾸지 않는 입력 시트다. 스와이프·바깥 터치·취소 모두 같은 취소 의도를 전달한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemEditorSheet(
    state: ShoppingItemEditorUiState,
    onIntent: (ShoppingListUiIntent) -> Unit,
    purchased: Boolean,
    editingUsers: List<ShoppingCollaboratorUiModel> = emptyList(),
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    ModalBottomSheet(
        onDismissRequest = { onIntent(ShoppingListUiIntent.CancelEdit) },
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(Res.string.shopping_edit_title), style = MaterialTheme.typography.titleLarge)
            if (purchased) Text(stringResource(Res.string.shopping_editor_purchased),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (editingUsers.isNotEmpty()) Text(
                stringResource(Res.string.shopping_editing, editingUsers.joinToString(", ") { it.displayName }),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(value = state.nameInput, onValueChange = { onIntent(ShoppingListUiIntent.EditName(it)) },
                label = { Text(stringResource(Res.string.shopping_quick_name)) }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, isError = state.nameError != null,
                supportingText = state.nameError?.let { error -> { Text(shoppingNameErrorText(error)) } })
            Text(stringResource(Res.string.shopping_category_optional), style = MaterialTheme.typography.labelLarge)
            ShoppingCategorySelector(state.category) { onIntent(ShoppingListUiIntent.EditCategory(it)) }
            OutlinedTextField(value = state.quantityInput, onValueChange = { onIntent(ShoppingListUiIntent.EditQuantity(it)) },
                label = { Text(stringResource(Res.string.shopping_quantity)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                supportingText = { Text(stringResource(Res.string.shopping_quantity_hint)) })
            OutlinedTextField(value = state.memoInput, onValueChange = { onIntent(ShoppingListUiIntent.EditMemo(it)) },
                label = { Text(stringResource(Res.string.shopping_memo)) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
            Text(stringResource(Res.string.shopping_temporary_notice), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onIntent(ShoppingListUiIntent.CancelEdit) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.shopping_cancel))
                }
                Button(onClick = {
                    focus.clearFocus(); keyboard?.hide()
                    onIntent(ShoppingListUiIntent.SubmitEdit)
                }, enabled = state.hasChanges, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.shopping_apply))
                }
            }

        }
    }
}
