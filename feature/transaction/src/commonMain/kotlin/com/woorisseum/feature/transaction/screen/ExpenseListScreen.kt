package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.feature.transaction.model.ExpenseItemUiModel
import com.woorisseum.feature.transaction.model.ExpenseListUiIntent
import com.woorisseum.feature.transaction.model.ExpenseListUiState
import com.woorisseum.feature.transaction.resources.*
import kotlinx.datetime.number
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 상태를 표시하고 Intent만 전달한다. ViewModel·DI·데이터 조회는 Route에서 연결한다. */
@Composable
fun ExpenseListScreen(
    state: ExpenseListUiState,
    onIntent: (ExpenseListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            ExpenseAddBar(onClick = { onIntent(ExpenseListUiIntent.AddExpense) })
        },
    ) { contentPadding ->
        Column(Modifier.fillMaxSize().padding(contentPadding)) {
            ExpenseHeader(state = state, onIntent = onIntent)
            ExpenseCategoryTabs(
                selectedCategory = state.filters.category,
                onSelect = { onIntent(ExpenseListUiIntent.SelectCategory(it)) },
            )
            ExpenseListFilterBar(state = state, onIntent = onIntent)
            if (state.dayGroups.isEmpty()) {
                ExpenseEmptyContent(
                    isFiltered = state.hasExpensesInMonth && state.filters.isActive,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                ) {
                    state.dayGroups.forEach { group ->
                        item(key = "day:${group.date}", contentType = "day-header") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = group.dateLabel,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    text = stringResource(Res.string.expense_amount_won, group.totalAmountText),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        items(
                            items = group.expenses,
                            key = { "expense:${it.id}" },
                            contentType = { "expense-row" },
                        ) { expense ->
                            ExpenseRow(expense = expense, onClick = { onIntent(ExpenseListUiIntent.OpenExpense(expense.id)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseHeader(
    state: ExpenseListUiState,
    onIntent: (ExpenseListUiIntent) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.expense_list_title),
                modifier = Modifier.weight(1f).widthIn(min = 64.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onIntent(ExpenseListUiIntent.ViewCards) }) {
                Text(stringResource(Res.string.card_management_title))
            }
            TextButton(onClick = { onIntent(ExpenseListUiIntent.ViewShopping) }) {
                Text(stringResource(Res.string.expense_open_shopping))
            }
            TextButton(onClick = { onIntent(ExpenseListUiIntent.ViewFixedExpenses) }) {
                Text(stringResource(Res.string.fixed_expense_title))
            }
            TextButton(onClick = { onIntent(ExpenseListUiIntent.ViewBudget) }) {
                Text(stringResource(Res.string.expense_view_budget))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.expense_month, state.month.year, state.month.month.number),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            IconButton(onClick = { onIntent(ExpenseListUiIntent.PreviousMonth) }) {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_left),
                    contentDescription = stringResource(Res.string.expense_previous_month),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { onIntent(ExpenseListUiIntent.NextMonth) }) {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = stringResource(Res.string.expense_next_month),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = stringResource(Res.string.expense_month_total, state.month.month.number),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        val currency = stringResource(Res.string.expense_currency_won)
        Text(
            text = buildAnnotatedString {
                append(state.totalAmountText)
                withStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal)) {
                    append(currency)
                }
            },
            style = MaterialTheme.typography.displaySmall,
            fontSize = 38.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1).sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.expense_count, state.expenseCount),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            if (state.periodText.isNotEmpty()) {
                Text(
                    text = state.periodText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ExpenseCategoryTabs(
    selectedCategory: ExpenseCategory?,
    onSelect: (ExpenseCategory?) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            (listOf(null) + ExpenseCategory.entries).forEach { category ->
                val selected = category == selectedCategory
                Column(
                    modifier = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(category) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        text = categoryLabel(category),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Box(
                        modifier = Modifier.width(48.dp).height(2.dp)
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseItemUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = expense.merchant.ifBlank { stringResource(Res.string.expense_unknown_merchant) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(
                    Res.string.expense_entry_metadata,
                    categoryLabel(expense.category),
                    expense.paymentMethodName,
                ),
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(Res.string.expense_amount_won, expense.amountText),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 160.dp),
        )
    }
}

@Composable
private fun ExpenseEmptyContent(isFiltered: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                if (isFiltered) Res.string.expense_filter_empty_title else Res.string.expense_empty_title,
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (isFiltered) Res.string.expense_filter_empty_description
                else Res.string.expense_empty_description,
            ),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExpenseAddBar(onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(painter = painterResource(Res.drawable.ic_plus), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(Res.string.expense_add))
            }
        }
    }
}

@Composable
private fun categoryLabel(category: ExpenseCategory?): String = stringResource(
    when (category) {
        null -> Res.string.expense_category_all
        ExpenseCategory.FIXED -> Res.string.expense_category_fixed
        ExpenseCategory.VARIABLE -> Res.string.expense_category_variable
        ExpenseCategory.IMPULSE -> Res.string.expense_category_impulse
    },
)
