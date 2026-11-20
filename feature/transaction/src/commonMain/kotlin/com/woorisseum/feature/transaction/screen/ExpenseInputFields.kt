package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.woorisseum.core.designsystem.component.WoorisseumTextField
import com.woorisseum.feature.transaction.model.ExpenseAmountInputError
import com.woorisseum.feature.transaction.resources.*
import org.jetbrains.compose.resources.stringResource

/** 실제 지출·고정지출 계획이 공유하는 금액 입력 UI다. 검증 결과는 부모가 전달한다. */
@Composable
internal fun ExpenseAmountInput(value: String, error: ExpenseAmountInputError?, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    WoorisseumTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.expense_amount_label)) },
        suffix = { Text(stringResource(Res.string.expense_currency_won)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        isError = error != null,
        supportingText = error?.let { error ->
            { Text(amountErrorText(error)) }
        },
    )
}

@Composable
internal fun ExpenseMemoInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    WoorisseumTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.expense_memo_label)) },
        minLines = 2,
        maxLines = 5,
    )
}

@Composable
private fun amountErrorText(error: ExpenseAmountInputError): String = stringResource(when (error) {
    ExpenseAmountInputError.REQUIRED -> Res.string.expense_amount_required
    ExpenseAmountInputError.INVALID_INTEGER -> Res.string.expense_amount_integer
    ExpenseAmountInputError.OUT_OF_RANGE -> Res.string.expense_amount_range
    ExpenseAmountInputError.NOT_POSITIVE -> Res.string.expense_amount_positive
})
