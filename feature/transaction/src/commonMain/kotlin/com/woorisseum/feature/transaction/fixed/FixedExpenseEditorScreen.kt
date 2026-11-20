package com.woorisseum.feature.transaction.fixed

import com.woorisseum.core.designsystem.component.WoorisseumTopAppBar
import com.woorisseum.core.designsystem.theme.WoorisseumSpacing
import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import com.woorisseum.core.designsystem.component.WoorisseumTextField
import com.woorisseum.core.designsystem.component.WoorisseumFilterChip
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
import com.woorisseum.core.designsystem.component.WoorisseumBottomActionBar
import com.woorisseum.core.designsystem.component.WoorisseumPrimaryButton
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
        topBar = { WoorisseumTopAppBar(
            title = { Text(stringResource(if (state.planId == null)
                Res.string.fixed_expense_register_title else Res.string.fixed_expense_edit_title), style = MaterialTheme.typography.headlineSmall) },
            navigationIcon = { WoorisseumTextButton(onClick = { onIntent(FixedExpenseEditorUiIntent.Cancel) }) {
                Text(stringResource(Res.string.expense_cancel))
            } },
        ) },
        bottomBar = {
            WoorisseumBottomActionBar {
                WoorisseumPrimaryButton(
                    enabled = state.form != null && (state.planId == null || state.hasChanges),
                    onClick = {
                        focus.clearFocus(); keyboard?.hide()
                        onIntent(FixedExpenseEditorUiIntent.Submit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(if (state.planId == null) Res.string.expense_save else Res.string.expense_edit_submit)) }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = WoorisseumSpacing.screenHorizontal, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            val form = state.form
            if (form == null) {
                FixedExpenseUnavailableContent()
            } else {
                Column {
                    Text(stringResource(Res.string.fixed_expense_type_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FixedExpenseType.entries.forEach { type ->
                            WoorisseumFilterChip(selected = form.type == type,
                                onClick = { onIntent(FixedExpenseEditorUiIntent.TypeSelected(type)) },
                                label = { Text(fixedExpenseTypeLabel(type)) })
                        }
                    }
                    if (form.typeRequired) Text(stringResource(Res.string.fixed_expense_type_required),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                WoorisseumTextField(
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
                WoorisseumTextField(
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
