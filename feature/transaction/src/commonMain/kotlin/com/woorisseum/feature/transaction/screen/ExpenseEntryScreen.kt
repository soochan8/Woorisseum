package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.feature.transaction.model.ExpenseAmountInputError
import com.woorisseum.feature.transaction.model.ExpenseEntryUiIntent
import com.woorisseum.feature.transaction.model.ExpenseEntryUiState
import com.woorisseum.feature.transaction.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

/** 입력값을 상태로 받고, 사용자 동작은 Intent로 전달한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryScreen(
    state: ExpenseEntryUiState,
    onIntent: (ExpenseEntryUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 날짜 선택창의 임시 선택은 확인 전까지 입력 상태에 반영하지 않는다.
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.expense_entry_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(Res.string.expense_back)) }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onIntent(ExpenseEntryUiIntent.Submit)
                    },
                    modifier = Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) { Text(stringResource(Res.string.expense_save)) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = { onIntent(ExpenseEntryUiIntent.AmountChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.expense_amount_label)) },
                suffix = { Text(stringResource(Res.string.expense_currency_won)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                isError = state.amountError != null,
                supportingText = state.amountError?.let { error ->
                    { Text(amountErrorText(error)) }
                },
            )
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
                onValueChange = { onIntent(ExpenseEntryUiIntent.MerchantChanged(it)) },
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
                            onClick = { onIntent(ExpenseEntryUiIntent.CategorySelected(category)) },
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
                    FilterChip(
                        selected = state.paymentMethod == PaymentMethod.Cash,
                        onClick = { onIntent(ExpenseEntryUiIntent.PaymentMethodSelected(PaymentMethod.Cash)) },
                        label = { Text(stringResource(Res.string.expense_payment_cash)) },
                    )
                    FilterChip(
                        selected = state.paymentMethod == PaymentMethod.BankTransfer,
                        onClick = { onIntent(ExpenseEntryUiIntent.PaymentMethodSelected(PaymentMethod.BankTransfer)) },
                        label = { Text(stringResource(Res.string.expense_payment_transfer)) },
                    )
                }
                if (state.paymentMethodRequired) {
                    Text(
                        stringResource(Res.string.expense_payment_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (state.showStorageUnavailable) {
                Text(
                    stringResource(Res.string.expense_storage_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    if (showDatePicker) {
        ExpenseDateDialog(
            date = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onIntent(ExpenseEntryUiIntent.DateChanged(date))
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun amountErrorText(error: ExpenseAmountInputError): String = stringResource(when (error) {
    ExpenseAmountInputError.REQUIRED -> Res.string.expense_amount_required
    ExpenseAmountInputError.INVALID_INTEGER -> Res.string.expense_amount_integer
    ExpenseAmountInputError.OUT_OF_RANGE -> Res.string.expense_amount_range
    ExpenseAmountInputError.NOT_POSITIVE -> Res.string.expense_amount_positive
})

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
