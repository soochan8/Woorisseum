package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.feature.transaction.model.ExpenseAmountInputError
import com.woorisseum.feature.transaction.model.ExpenseFormUiIntent
import com.woorisseum.feature.transaction.model.ExpenseFormUiState
import com.woorisseum.feature.transaction.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

/** 등록·수정 공통 입력 UI다. 저장과 화면 이동은 부모 화면이 담당한다. */
@Composable
internal fun ExpenseForm(
    state: ExpenseFormUiState,
    onIntent: (ExpenseFormUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ExpenseAmountInput(state.amountInput, state.amountError, { onIntent(ExpenseFormUiIntent.AmountChanged(it)) })
        Column {
            Text(stringResource(Res.string.expense_date_label), style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { showDatePicker = true }) {
                Text(stringResource(
                    Res.string.expense_date_value, state.date.year, state.date.month.number, state.date.day,
                ))
            }
        }
        OutlinedTextField(
            value = state.merchantInput,
            onValueChange = { onIntent(ExpenseFormUiIntent.MerchantChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.expense_merchant_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.expense_category_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpenseCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { onIntent(ExpenseFormUiIntent.CategorySelected(category)) },
                        label = { Text(stringResource(when (category) {
                            ExpenseCategory.FIXED -> Res.string.expense_category_fixed
                            ExpenseCategory.VARIABLE -> Res.string.expense_category_variable
                            ExpenseCategory.IMPULSE -> Res.string.expense_category_impulse
                        })) },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.expense_payment_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availablePaymentMethods.forEach { method ->
                    FilterChip(
                        selected = state.paymentMethod == method,
                        onClick = { onIntent(ExpenseFormUiIntent.PaymentMethodSelected(method)) },
                        label = { Text(when (method) {
                            PaymentMethod.Cash -> stringResource(Res.string.expense_payment_cash)
                            PaymentMethod.BankTransfer -> stringResource(Res.string.expense_payment_transfer)
                            is PaymentMethod.Card -> method.name
                        }) },
                    )
                }
            }
            if (state.paymentMethodRequired) {
                Text(
                    stringResource(Res.string.expense_payment_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        ExpenseMemoInput(state.memoInput, { onIntent(ExpenseFormUiIntent.MemoChanged(it)) })
    }
    if (showDatePicker) {
        ExpenseDateDialog(
            date = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onIntent(ExpenseFormUiIntent.DateChanged(date))
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDateDialog(date: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    // Material 날짜 선택기의 millis는 UTC 날짜다. 기기 시간대로 변환하면 날짜가 바뀔 수 있다.
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDays() * 86_400_000L)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onConfirm(LocalDate.fromEpochDays(millis / 86_400_000L))
                    }
                },
            ) { Text(stringResource(Res.string.expense_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.expense_cancel)) }
        },
    ) { DatePicker(state = pickerState) }
}
