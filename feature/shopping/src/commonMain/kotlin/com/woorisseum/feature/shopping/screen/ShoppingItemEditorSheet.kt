package com.woorisseum.feature.shopping.screen

import com.woorisseum.core.designsystem.component.WoorisseumPrimaryButton
import com.woorisseum.core.designsystem.component.WoorisseumSecondaryButton
import com.woorisseum.core.designsystem.component.WoorisseumTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.woorisseum.feature.shopping.model.*
import com.woorisseum.feature.shopping.resources.*
import org.jetbrains.compose.resources.stringResource

/** 초안은 ViewModel이 보관한다. 키보드 닫기와 시트 취소를 분리해 입력 중 이탈을 막는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemEditorSheet(
    state: ShoppingItemEditorUiState,
    onIntent: (ShoppingListUiIntent) -> Unit,
    purchased: Boolean,
    editingUsers: List<ShoppingCollaboratorUiModel> = emptyList(),
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val keyboardVisible = imeInsets.getBottom(density) > 0
    // 안정적인 콜백을 사용해 키보드 표시 때 SheetState가 다시 만들어지지 않게 한다.
    val confirmSheetChange = remember(imeInsets, density, keyboard) {
        { target: SheetValue ->
            if (target == SheetValue.Hidden && imeInsets.getBottom(density) > 0) {
                keyboard?.hide()
                false
            } else true
        }
    }
    ModalBottomSheet(
        onDismissRequest = { keyboard?.hide(); onIntent(ShoppingListUiIntent.CancelEdit) },
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded), confirmValueChange = confirmSheetChange),
        // 키보드가 열린 동안 시작한 드래그가 시트 닫기로 이어지지 않게 한다.
        sheetGesturesEnabled = !keyboardVisible,
        dragHandle = {
            BottomSheetDefaults.DragHandle(modifier = if (keyboardVisible) Modifier.pointerInput(keyboard) {
                detectVerticalDragGestures { change, distance ->
                    if (distance > 0) keyboard?.hide()
                    change.consume()
                }
            } else Modifier)
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // 시트가 소유한 포커스를 해제한다. 부모 화면의 FocusManager와 구분한다.
        val focus = LocalFocusManager.current
        val sheetKeyboard = LocalSoftwareKeyboardController.current
        val hideKeyboard: () -> Unit = {
            focus.clearFocus()
            sheetKeyboard?.hide()
        }
        val scrollState = rememberScrollState()
        val dragging by scrollState.interactionSource.collectIsDraggedAsState()
        LaunchedEffect(dragging) { if (dragging) hideKeyboard() }
        LaunchedEffect(keyboardVisible) { if (!keyboardVisible) focus.clearFocus() }
        Column(Modifier.fillMaxWidth().imePadding().pointerInput(focus, sheetKeyboard) {
            detectTapGestures(onTap = { focus.clearFocus(); sheetKeyboard?.hide() })
        }) {
            Text(stringResource(Res.string.shopping_edit_title), Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleLarge)
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(scrollState)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (purchased) Text(stringResource(Res.string.shopping_editor_purchased),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (editingUsers.isNotEmpty()) Text(
                    stringResource(Res.string.shopping_editing, editingUsers.joinToString(", ") { it.displayName }),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                WoorisseumTextField(value = state.nameInput, onValueChange = { onIntent(ShoppingListUiIntent.EditName(it)) },
                    label = { Text(stringResource(Res.string.shopping_quick_name)) }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, isError = state.nameError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                    supportingText = state.nameError?.let { error -> { Text(shoppingNameErrorText(error)) } })
                Text(stringResource(Res.string.shopping_category_optional), style = MaterialTheme.typography.labelLarge)
                ShoppingCategorySelector(state.category, onBeforeOpen = hideKeyboard) { onIntent(ShoppingListUiIntent.EditCategory(it)) }
                WoorisseumTextField(value = state.quantityInput, onValueChange = { onIntent(ShoppingListUiIntent.EditQuantity(it)) },
                    label = { Text(stringResource(Res.string.shopping_quantity)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                    supportingText = { Text(stringResource(Res.string.shopping_quantity_hint)) })
                WoorisseumTextField(value = state.memoInput, onValueChange = { onIntent(ShoppingListUiIntent.EditMemo(it)) },
                    label = { Text(stringResource(Res.string.shopping_memo)) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
                Text(stringResource(Res.string.shopping_temporary_notice), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WoorisseumSecondaryButton(onClick = { hideKeyboard(); onIntent(ShoppingListUiIntent.CancelEdit) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.shopping_cancel))
                    }
                    WoorisseumPrimaryButton(onClick = {
                        hideKeyboard()
                        onIntent(ShoppingListUiIntent.SubmitEdit)
                    }, enabled = state.hasChanges, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.shopping_apply))
                    }
                }
            }
        }
    }
}
