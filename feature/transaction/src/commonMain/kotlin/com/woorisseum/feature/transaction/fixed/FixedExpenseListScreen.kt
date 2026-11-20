package com.woorisseum.feature.transaction.fixed

import com.woorisseum.core.designsystem.theme.WoorisseumSpacing
import com.woorisseum.core.designsystem.component.WoorisseumBottomActionBar
import com.woorisseum.core.designsystem.component.WoorisseumPrimaryButton
import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woorisseum.core.domain.transaction.fixed.FixedExpensePaymentStatus
import com.woorisseum.core.domain.transaction.fixed.FixedExpenseType
import com.woorisseum.feature.transaction.resources.*
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 월별 예정 계획만 표시한다. 납부 기록 변경이나 실제 지출 생성은 하지 않는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedExpenseListScreen(
    state: FixedExpenseListUiState,
    onIntent: (FixedExpenseListUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.fixed_expense_title)) },
            navigationIcon = { WoorisseumTextButton(onClick = onBack) { Text(stringResource(Res.string.expense_back)) } }) },
        bottomBar = {
            WoorisseumBottomActionBar {
                WoorisseumPrimaryButton(onClick = { onIntent(FixedExpenseListUiIntent.AddPlan) },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.fixed_expense_add))
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = WoorisseumSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                FixedExpenseMonthSelector(state.month,
                    { onIntent(FixedExpenseListUiIntent.PreviousMonth) }, { onIntent(FixedExpenseListUiIntent.NextMonth) })
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.fixed_expense_total), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.summary?.totalAmountText?.let { stringResource(Res.string.expense_amount_won, it) }
                        ?: stringResource(Res.string.fixed_expense_waiting),
                        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                    Text(state.summary?.paidAmountText?.let { stringResource(Res.string.fixed_expense_paid_total, it) }
                        ?: stringResource(Res.string.fixed_expense_payment_waiting),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            val summary = state.summary
            if (summary == null) {
                item { FixedExpenseUnavailableContent() }
            } else if (summary.items.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(Res.string.fixed_expense_empty_title), textAlign = TextAlign.Center)
                        Text(stringResource(Res.string.fixed_expense_empty_description), textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(summary.items, key = { it.id }) { plan ->
                    FixedExpenseRow(plan) { onIntent(FixedExpenseListUiIntent.EditPlan(plan.id)) }
                }
            }
            item { Text(stringResource(Res.string.fixed_expense_payment_notice),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun FixedExpenseRow(item: FixedExpenseItemUiModel, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name ?: fixedExpenseTypeLabel(item.type), fontWeight = FontWeight.Medium)
            Text(stringResource(Res.string.fixed_expense_item_meta, fixedExpenseTypeLabel(item.type),
                item.dueDate.month.number, item.dueDate.day), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.widthIn(max = 160.dp), horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.expense_amount_won, item.amountText), textAlign = TextAlign.End)
            Text(stringResource(when (item.paymentStatus) {
                FixedExpensePaymentStatus.PAID -> Res.string.fixed_expense_paid
                FixedExpensePaymentStatus.UNPAID -> Res.string.fixed_expense_unpaid
                FixedExpensePaymentStatus.UNKNOWN -> Res.string.fixed_expense_unknown_payment
            }), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun FixedExpenseMonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.expense_month, month.year, month.month.number), Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = onPrevious) { Icon(painterResource(Res.drawable.ic_chevron_left),
            stringResource(Res.string.expense_previous_month), Modifier.size(20.dp)) }
        IconButton(onClick = onNext) { Icon(painterResource(Res.drawable.ic_chevron_right),
            stringResource(Res.string.expense_next_month), Modifier.size(20.dp)) }
    }
}

@Composable
internal fun FixedExpenseUnavailableContent(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.fixed_expense_lookup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.fixed_expense_lookup_description),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun fixedExpenseTypeLabel(type: FixedExpenseType): String = stringResource(when (type) {
    FixedExpenseType.RENT -> Res.string.fixed_expense_type_rent
    FixedExpenseType.TELECOMMUNICATION -> Res.string.fixed_expense_type_telecommunication
    FixedExpenseType.SUBSCRIPTION -> Res.string.fixed_expense_type_subscription
    FixedExpenseType.INSURANCE -> Res.string.fixed_expense_type_insurance
    FixedExpenseType.UTILITIES -> Res.string.fixed_expense_type_utilities
    FixedExpenseType.OTHER -> Res.string.fixed_expense_type_other
})
