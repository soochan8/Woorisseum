package com.woorisseum.feature.shopping.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woorisseum.feature.shopping.model.ShoppingPresenceUiState
import com.woorisseum.feature.shopping.model.ShoppingListUiIntent
import com.woorisseum.feature.shopping.viewmodel.ShoppingListViewModel
import org.koin.compose.viewmodel.koinViewModel

/** DI·생명주기 수집과 목록/수정 시트를 연결한다. 임시 입력의 원본은 ViewModel 한 곳에 둔다. */
@Composable
fun ShoppingListRoute(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    viewModel: ShoppingListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingListScreen(state, viewModel::onIntent, onBack, modifier, onOpenHistory = onOpenHistory, showBackButton = showBackButton)
    if (state.finishItemIds != null) {
        ShoppingFinishDialog(state.itemsToFinish.size, state.items.size - state.itemsToFinish.size,
            onCancel = { viewModel.onIntent(ShoppingListUiIntent.CancelFinish) },
            onConfirm = { viewModel.onIntent(ShoppingListUiIntent.ConfirmFinish) })
    }
    state.editor?.let { editor ->
        state.currentEditedItem?.let { current ->
            ShoppingItemEditorSheet(editor, viewModel::onIntent, purchased = current.purchased,
                editingUsers = (state.presence as? ShoppingPresenceUiState.Connected)?.editing?.get(current.id).orEmpty())
        }
    }
}
