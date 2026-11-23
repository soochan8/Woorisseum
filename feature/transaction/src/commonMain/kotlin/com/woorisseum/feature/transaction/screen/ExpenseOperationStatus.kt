package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import com.woorisseum.feature.transaction.model.ExpenseOperationError
import com.woorisseum.feature.transaction.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun expenseErrorText(error: ExpenseOperationError): String = stringResource(when (error) {
    ExpenseOperationError.STORAGE -> Res.string.expense_db_failed
    ExpenseOperationError.NOT_FOUND -> Res.string.expense_not_found
    ExpenseOperationError.CONFLICT -> Res.string.expense_edit_conflict
    ExpenseOperationError.INVALID_INPUT -> Res.string.expense_invalid_input
})

/** 조회 실패를 빈 목록이나 준비 중 안내로 대신하지 않는다. */
@Composable
internal fun ExpenseOperationStatus(isLoading: Boolean, error: ExpenseOperationError?, onRetry: () -> Unit,
    modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (isLoading) CircularProgressIndicator()
        else if (error != null) {
            Text(expenseErrorText(error), color = MaterialTheme.colorScheme.error)
            if (error != ExpenseOperationError.NOT_FOUND) {
                WoorisseumTextButton(onClick = onRetry) { Text(stringResource(Res.string.expense_retry)) }
            }
        }
    }
}

@Composable
internal fun expenseSaveErrorText(error: ExpenseOperationError): String =
    if (error == ExpenseOperationError.STORAGE) stringResource(Res.string.expense_save_failed)
    else expenseErrorText(error)
