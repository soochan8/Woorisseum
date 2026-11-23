package com.woorisseum.feature.shopping.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import com.woorisseum.feature.shopping.model.*
import com.woorisseum.feature.shopping.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ShoppingStorageStatus(state: ShoppingListUiState, onIntent: (ShoppingListUiIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.isLoading || state.isSaving) Text(stringResource(if (state.isSaving) Res.string.shopping_saving else Res.string.shopping_loading),
            style = MaterialTheme.typography.bodySmall)
        state.loadError?.let {
            Text(stringResource(Res.string.shopping_load_error), color = MaterialTheme.colorScheme.error)
            WoorisseumTextButton(onClick = { onIntent(ShoppingListUiIntent.Retry) }, enabled = !state.isSaving) {
                Text(stringResource(Res.string.shopping_retry))
            }
        }
        state.operationError?.let { ShoppingOperationError(it) }
    }
}

@Composable
internal fun ShoppingOperationError(error: ShoppingStorageError) {
    Text(stringResource(when (error) {
        ShoppingStorageError.STORAGE -> Res.string.shopping_save_error
        ShoppingStorageError.NOT_FOUND -> Res.string.shopping_not_found
        ShoppingStorageError.CONFLICT -> Res.string.shopping_conflict
        ShoppingStorageError.INVALID_INPUT -> Res.string.shopping_invalid_input
    }), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
