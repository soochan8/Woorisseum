package com.woorisseum.feature.budget.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.feature.budget.model.BudgetAmountInputError
import com.woorisseum.feature.budget.model.BudgetCategoryUiModel
import com.woorisseum.feature.budget.model.BudgetHeadline
import com.woorisseum.feature.budget.model.BudgetSummaryUiModel
import com.woorisseum.feature.budget.model.BudgetUsage
import com.woorisseum.feature.budget.model.MonthlyBudgetUiIntent
import com.woorisseum.feature.budget.model.MonthlyBudgetUiState
import com.woorisseum.feature.budget.resources.*
import kotlinx.datetime.number
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 표시 상태와 콜백만 사용한다. 금액 계산·데이터 조회·DI는 화면 밖에서 담당한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyBudgetScreen(
    state: MonthlyBudgetUiState,
    onIntent: (MonthlyBudgetUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.budget_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showBackButton) IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.budget_chevron_left),
                            stringResource(Res.string.budget_back))
                    }
                },
                actions = {
                    Text(stringResource(Res.string.budget_app_name),
                        Modifier.padding(end = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                TextButton(
                    onClick = { onIntent(MonthlyBudgetUiIntent.ViewExpenses) },
                    modifier = Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 8.dp).heightIn(min = 48.dp),
                ) { Text(stringResource(Res.string.budget_view_expenses)) }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.budget_month, state.month.year, state.month.month.number),
                        Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { onIntent(MonthlyBudgetUiIntent.PreviousMonth) }) {
                        Icon(painterResource(Res.drawable.budget_chevron_left),
                            stringResource(Res.string.budget_previous_month), Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onIntent(MonthlyBudgetUiIntent.NextMonth) }) {
                        Icon(painterResource(Res.drawable.budget_chevron_right),
                            stringResource(Res.string.budget_next_month), Modifier.size(20.dp))
                    }
                }
            }
            item { BudgetHeadlineContent(state.summary) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.budget_total), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium)
                        Text(state.summary.budgetAmountText?.let { stringResource(Res.string.budget_won, it) }
                            ?: stringResource(Res.string.budget_unset), style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = { onIntent(MonthlyBudgetUiIntent.EditBudget) }) {
                        Text(stringResource(if (state.summary.budgetAmountText == null)
                            Res.string.budget_set else Res.string.budget_change))
                    }
                }
            }
            item { BudgetUsageContent(state.summary) }
            item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            item { Text(stringResource(Res.string.budget_categories_title), style = MaterialTheme.typography.titleMedium) }
            items(state.summary.categories, key = { it.category.name }) { category -> BudgetCategoryContent(category) }
            if (state.summary.spentAmountText == null) {
                item { Text(stringResource(Res.string.budget_waiting_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
            if (state.isTemporaryBudget) {
                item { Text(stringResource(Res.string.budget_temporary_notice),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
            item { Box(Modifier.height(12.dp)) }
        }
    }
    state.editor?.let { editor ->
        AlertDialog(
            onDismissRequest = { onIntent(MonthlyBudgetUiIntent.CancelEditing) },
            title = { Text(stringResource(Res.string.budget_editor_title, state.month.month.number)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editor.amountInput,
                        onValueChange = { onIntent(MonthlyBudgetUiIntent.AmountChanged(it)) },
                        label = { Text(stringResource(Res.string.budget_total)) },
                        suffix = { Text(stringResource(Res.string.budget_currency)) },
                        singleLine = true,
                        isError = editor.error != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        supportingText = editor.error?.let { error -> { Text(budgetErrorText(error)) } },
                    )
                    Text(stringResource(Res.string.budget_editor_notice),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(MonthlyBudgetUiIntent.ApplyBudget) }) {
                    Text(stringResource(Res.string.budget_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(MonthlyBudgetUiIntent.CancelEditing) }) {
                    Text(stringResource(Res.string.budget_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun BudgetHeadlineContent(summary: BudgetSummaryUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(when (summary.headline) {
            BudgetHeadline.UNSET -> Res.string.budget_headline_unset
            BudgetHeadline.WAITING, BudgetHeadline.REMAINING -> Res.string.budget_headline_remaining
            BudgetHeadline.EXCEEDED -> Res.string.budget_headline_exceeded
            BudgetHeadline.ZERO -> Res.string.budget_headline_zero
        }), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        val amount = summary.headlineAmountText
        if (amount == null) {
            Text(stringResource(Res.string.budget_waiting), style = MaterialTheme.typography.headlineMedium)
        } else {
            val currency = stringResource(Res.string.budget_currency)
            Text(buildAnnotatedString {
                append(amount)
                withStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal)) { append(currency) }
            }, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold,
                color = if (summary.headline == BudgetHeadline.EXCEEDED) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BudgetUsageContent(summary: BudgetSummaryUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(summary.spentAmountText?.let { stringResource(Res.string.budget_spent, it) }
            ?: stringResource(Res.string.budget_spent_waiting), style = MaterialTheme.typography.bodyMedium)
        BudgetProgress(summary.progress, known = summary.usage == BudgetUsage.AVAILABLE,
            label = stringResource(Res.string.budget_usage_label))
        Text(stringResource(when (summary.usage) {
            BudgetUsage.UNSET -> Res.string.budget_usage_unset
            BudgetUsage.WAITING -> Res.string.budget_usage_waiting
            BudgetUsage.ZERO_BUDGET -> Res.string.budget_usage_zero
            BudgetUsage.AVAILABLE -> if (summary.isUsageAboveDisplayLimit)
                Res.string.budget_usage_above_limit else Res.string.budget_usage_percent
        }, summary.usagePercentText.orEmpty()),
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BudgetCategoryContent(category: BudgetCategoryUiModel) {
    val categoryName = stringResource(when (category.category) {
        ExpenseCategory.FIXED -> Res.string.budget_category_fixed
        ExpenseCategory.VARIABLE -> Res.string.budget_category_variable
        ExpenseCategory.IMPULSE -> Res.string.budget_category_impulse
    })
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(category.sharePercentText?.let {
                stringResource(Res.string.budget_category_with_share, categoryName, it)
            } ?: categoryName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(category.amountText?.let { stringResource(Res.string.budget_won, it) }
                ?: stringResource(Res.string.budget_waiting), style = MaterialTheme.typography.bodyMedium)
        }
        BudgetProgress(category.progress, known = category.amountText != null,
            label = stringResource(Res.string.budget_category_share))
    }
}

@Composable
private fun BudgetProgress(value: Float, known: Boolean, label: String) {
    if (known) {
        LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth().height(6.dp)
            .semantics { contentDescription = label },
            color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant,
            drawStopIndicator = {})
    } else {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outlineVariant))
    }
}

@Composable
private fun budgetErrorText(error: BudgetAmountInputError): String = stringResource(when (error) {
    BudgetAmountInputError.REQUIRED -> Res.string.budget_error_required
    BudgetAmountInputError.INVALID_INTEGER -> Res.string.budget_error_integer
    BudgetAmountInputError.OUT_OF_RANGE -> Res.string.budget_error_range
})
