package com.woorisseum.feature.shopping.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woorisseum.core.designsystem.component.*
import com.woorisseum.core.domain.shopping.ShoppingCategoryNameError
import com.woorisseum.feature.shopping.model.*
import com.woorisseum.feature.shopping.resources.*
import com.woorisseum.feature.shopping.viewmodel.ShoppingListViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 장보기와 같은 상태 소유자를 사용해 변경을 즉시 반영한다. 저장소·DB를 임의로 만들지 않는다. */
@Composable
fun ShoppingCategoryRoute(onBack: () -> Unit, viewModelStoreOwner: ViewModelStoreOwner) {
    val viewModel: ShoppingListViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    InitializeShoppingCategories(viewModel::onIntent)
    LaunchedEffect(viewModel) { viewModel.onIntent(ShoppingListUiIntent.CloseCategoryManagement) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingCategoryScreen(state, viewModel::onIntent, onBack)
}

@Composable
fun ShoppingCategoryScreen(
    state: ShoppingListUiState,
    onIntent: (ShoppingListUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealedId by remember { mutableStateOf<String?>(null) }
    val usage = state.categoryUsage
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { WoorisseumTopAppBar(title = { Text(stringResource(Res.string.shopping_categories_title)) },
            navigationIcon = { WoorisseumTextButton(onClick = onBack) { Text(stringResource(Res.string.shopping_back)) } }) },
        bottomBar = { WoorisseumBottomActionBar {
            WoorisseumPrimaryButton(onClick = { revealedId = null; onIntent(ShoppingListUiIntent.OpenCategoryEditor()) },
                enabled = state.categoriesInitialized, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.shopping_category_add))
            }
        } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "notice") { Text(stringResource(Res.string.shopping_categories_notice),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (state.categoriesInitialized && state.categories.isEmpty()) item(key = "empty") {
                Text(stringResource(Res.string.shopping_categories_empty))
            }
            items(state.categories, key = { it.category.id }) { definition ->
                val id = definition.category
                SwipeToRevealDeleteRow(id.id, revealedId == id.id, { revealedId = it },
                    stringResource(Res.string.shopping_category_delete_action, definition.name), stringResource(Res.string.shopping_delete),
                    onDelete = { revealedId = null; onIntent(ShoppingListUiIntent.RequestDeleteCategory(id)) }) {
                    Column(Modifier.fillMaxWidth().clickable(role = Role.Button,
                        onClickLabel = stringResource(Res.string.shopping_category_edit_action, definition.name),
                        onClick = { if (revealedId != null) revealedId = null else onIntent(ShoppingListUiIntent.OpenCategoryEditor(id)) })
                        .padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(definition.name, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(Res.string.shopping_category_usage, usage[id] ?: 0),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    state.categoryEditor?.let { draft ->
        AlertDialog(
            onDismissRequest = { onIntent(ShoppingListUiIntent.CancelCategoryEdit) },
            modifier = Modifier.imePadding(),
            title = { Text(stringResource(if (draft.original == null) Res.string.shopping_category_add else Res.string.shopping_category_edit)) },
            text = {
                WoorisseumTextField(draft.nameInput, { onIntent(ShoppingListUiIntent.CategoryNameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(Res.string.shopping_category_name)) }, isError = draft.error != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onIntent(ShoppingListUiIntent.SubmitCategory) }),
                    supportingText = draft.error?.let { error -> { Text(stringResource(when (error) {
                        ShoppingCategoryNameError.REQUIRED -> Res.string.shopping_category_name_required
                        ShoppingCategoryNameError.DUPLICATE -> Res.string.shopping_category_name_duplicate
                        ShoppingCategoryNameError.RESERVED -> Res.string.shopping_category_name_reserved
                    })) } })
            },
            confirmButton = { WoorisseumTextButton(onClick = { onIntent(ShoppingListUiIntent.SubmitCategory) }, enabled = draft.hasChanges) {
                Text(stringResource(Res.string.shopping_category_save))
            } },
            dismissButton = { WoorisseumTextButton(onClick = { onIntent(ShoppingListUiIntent.CancelCategoryEdit) }) {
                Text(stringResource(Res.string.shopping_cancel))
            } },
        )
    }
    state.categories.find { it.category == state.categoryToDelete }?.let { definition ->
        val count = usage[definition.category] ?: 0
        AlertDialog(
            onDismissRequest = { onIntent(ShoppingListUiIntent.CancelDeleteCategory) },
            title = { Text(stringResource(Res.string.shopping_category_delete_title, definition.name)) },
            text = { Text(if (count > 0) stringResource(Res.string.shopping_category_delete_used, count)
                else stringResource(Res.string.shopping_category_delete_unused)) },
            confirmButton = { WoorisseumTextButton(onClick = { onIntent(ShoppingListUiIntent.ConfirmDeleteCategory) }) {
                Text(stringResource(Res.string.shopping_delete), color = MaterialTheme.colorScheme.error)
            } },
            dismissButton = { WoorisseumTextButton(onClick = { onIntent(ShoppingListUiIntent.CancelDeleteCategory) }) {
                Text(stringResource(Res.string.shopping_cancel))
            } },
        )
    }
}
