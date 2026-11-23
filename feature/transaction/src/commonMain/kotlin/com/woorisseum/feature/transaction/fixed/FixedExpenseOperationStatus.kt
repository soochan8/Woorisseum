package com.woorisseum.feature.transaction.fixed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import com.woorisseum.feature.transaction.model.ExpenseOperationError
import com.woorisseum.feature.transaction.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun fixedExpenseErrorText(error: ExpenseOperationError): String = stringResource(when (error) {
    ExpenseOperationError.STORAGE -> Res.string.fixed_expense_db_error
    ExpenseOperationError.NOT_FOUND -> Res.string.fixed_expense_not_found
    ExpenseOperationError.CONFLICT -> Res.string.fixed_expense_conflict
    ExpenseOperationError.INVALID_INPUT -> Res.string.fixed_expense_invalid_input
})

@Composable
internal fun FixedExpenseOperationStatus(isLoading: Boolean, error: ExpenseOperationError?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        if (isLoading) CircularProgressIndicator()
        else if (error != null) {
            Text(fixedExpenseErrorText(error), color = MaterialTheme.colorScheme.error)
            if (error != ExpenseOperationError.NOT_FOUND) WoorisseumTextButton(onClick = onRetry) {
                Text(stringResource(Res.string.expense_retry))
            }
        }
    }
}
