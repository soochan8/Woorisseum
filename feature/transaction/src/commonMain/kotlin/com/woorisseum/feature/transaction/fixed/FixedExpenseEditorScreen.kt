package com.woorisseum.feature.transaction.fixed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woorisseum.core.domain.transaction.fixed.FixedExpenseType
import com.woorisseum.feature.transaction.resources.*
import com.woorisseum.feature.transaction.screen.ExpenseAmountInput
import com.woorisseum.feature.transaction.screen.ExpenseMemoInput
import org.jetbrains.compose.resources.stringResource

/** 실제 거래 폼과 다른 반복 계획 입력을 구성한다. 공통 금액·메모 입력은 재사용한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedExpenseEditorScreen(
    state: FixedExpenseEditorUiState,
    onIntent: (FixedExpenseEditorUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(
        modifier = modifier.imePadding(), containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(
            title = { Text(stringResource(if (state.planId == null)
                Res.string.fixed_expense_register_title else Res.string.fixed_expense_edit_title)) },
            navigationIcon = { TextButton(onClick = { onIntent(FixedExpenseEditorUiIntent.Cancel) }) {
                Text(stringResource(Res.string.expense_cancel))
            } },
        ) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    enabled = state.form != null && (state.planId == null || state.hasChanges),
                    onClick = {
                        focus.clearFocus(); keyboard?.hide()
                        onIntent(FixedExpenseEditorUiIntent.Submit)
                    },
                    modifier = Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) { Text(stringResource(if (state.planId == null) Res.string.expense_save else Res.string.expense_edit_submit)) }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            val form = state.form
            if (form == null) {
                FixedExpenseUnavailableContent()
            } else {
                Column {
                    Text(stringResource(Res.string.fixed_expense_type_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FixedExpenseType.entries.forEach { type ->
                            FilterChip(selected = form.type == type,
                                onClick = { onIntent(FixedExpenseEditorUiIntent.TypeSelected(type)) },
                                label = { Text(fixedExpenseTypeLabel(type)) })
                        }
                    }
                    if (form.typeRequired) Text(stringResource(Res.string.fixed_expense_type_required),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = form.nameInput,
                    onValueChange = { onIntent(FixedExpenseEditorUiIntent.NameChanged(it)) },
                    label = { Text(stringResource(if (form.type == FixedExpenseType.OTHER)
                        Res.string.fixed_expense_name_other_label else Res.string.fixed_expense_name_label)) },
                    supportingText = { Text(stringResource(if (form.type == FixedExpenseType.OTHER)
                        Res.string.fixed_expense_name_required else Res.string.fixed_expense_name_hint)) },
                    isError = form.nameRequired, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                ExpenseAmountInput(form.amountInput, form.amountError,
                    { onIntent(FixedExpenseEditorUiIntent.AmountChanged(it)) })
                OutlinedTextField(
                    value = form.billingDayInput,
                    onValueChange = { onIntent(FixedExpenseEditorUiIntent.BillingDayChanged(it)) },
                    label = { Text(stringResource(Res.string.fixed_expense_day_label)) },
                    suffix = { Text(stringResource(Res.string.fixed_expense_day_suffix)) },
                    supportingText = { Text(stringResource(if (form.invalidBillingDay)
                        Res.string.fixed_expense_day_error else Res.string.fixed_expense_day_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = form.invalidBillingDay, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    Text(stringResource(Res.string.fixed_expense_start_month), style = MaterialTheme.typography.labelLarge)
                    FixedExpenseMonthSelector(form.startMonth,
                        { onIntent(FixedExpenseEditorUiIntent.PreviousStartMonth) },
                        { onIntent(FixedExpenseEditorUiIntent.NextStartMonth) })
                    Text(stringResource(Res.string.fixed_expense_repeat_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ExpenseMemoInput(form.memoInput, { onIntent(FixedExpenseEditorUiIntent.MemoChanged(it)) })
                Text(stringResource(if (state.showStorageUnavailable)
                    Res.string.fixed_expense_storage_unavailable else Res.string.fixed_expense_editor_notice),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
