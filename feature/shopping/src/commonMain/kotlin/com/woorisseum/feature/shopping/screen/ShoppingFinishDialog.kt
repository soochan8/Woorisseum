package com.woorisseum.feature.shopping.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.woorisseum.feature.shopping.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ShoppingFinishDialog(completedCount: Int, remainingCount: Int, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.shopping_finish_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.shopping_finish_description, completedCount, remainingCount))
                Text(stringResource(Res.string.shopping_history_notice), style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(Res.string.shopping_cancel)) } },
        confirmButton = { TextButton(onClick = onConfirm, enabled = completedCount > 0) { Text(stringResource(Res.string.shopping_finish)) } })
}
