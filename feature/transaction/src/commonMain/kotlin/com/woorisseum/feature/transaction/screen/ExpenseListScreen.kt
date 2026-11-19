package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
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
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var requestSearchFocus by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { if (!state.isSearchOpen) ExpenseAddBar(onClick = { onIntent(ExpenseListUiIntent.AddExpense) }) },
    ) { contentPadding ->
        // 검색창·필터도 함께 스크롤하여 좁은 화면과 키보드 표시 중에 콘텐츠가 가려지지 않게 한다.
        LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "header", contentType = "header") {
                ExpenseHeader(state, onIntent, onToggleSearch = {
                    if (state.isSearchOpen) {
                        focusManager.clearFocus(); keyboard?.hide()
                        onIntent(ExpenseListUiIntent.CloseSearch)
                    } else {
                        requestSearchFocus = true
                        onIntent(ExpenseListUiIntent.OpenSearch)
                    }
                })
            }
            if (state.isSearchOpen) item(key = "search", contentType = "search") {
                ExpenseSearchField(state.searchQuery,
                    onQueryChange = { onIntent(ExpenseListUiIntent.SearchQueryChanged(it)) },
                    onClear = { onIntent(ExpenseListUiIntent.ClearSearch) },
                    requestFocus = requestSearchFocus, onFocusRequested = { requestSearchFocus = false })
            }
            item(key = "categories", contentType = "filters") {
                ExpenseCategoryTabs(state.filters.category) { onIntent(ExpenseListUiIntent.SelectCategory(it)) }
            }
            item(key = "filters", contentType = "filters") { ExpenseListFilterBar(state, onIntent) }
            if (state.isSearchOpen && state.isExpenseSourceReady) item(key = "search-count") {
                Text(stringResource(Res.string.expense_search_result_count, state.dayGroups.sumOf { it.expenses.size }),
                    Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelLarge)
            }
            if (state.dayGroups.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    when {
                        state.isSearchOpen && !state.isExpenseSourceReady -> SearchEmptyContent(
                            unavailable = true, onClear = { onIntent(ExpenseListUiIntent.ClearSearch) },
                            modifier = Modifier.fillParentMaxHeight(0.45f))
                        state.isSearchOpen -> SearchEmptyContent(
                            unavailable = false, onClear = { onIntent(ExpenseListUiIntent.ClearSearch) },
                            modifier = Modifier.fillParentMaxHeight(0.45f))
                        else -> ExpenseEmptyContent(state.hasExpensesInMonth && state.filters.isActive,
                            modifier = Modifier.fillParentMaxHeight(0.45f))
                    }
                }
            } else state.dayGroups.forEach { group ->
                item(key = "day:${group.date}", contentType = "day-header") {
                    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (state.isSearchOpen) stringResource(Res.string.expense_search_day_full, group.date.year, group.date.month.number, group.date.day)
                            else group.dateLabel.ifBlank { stringResource(Res.string.expense_search_day, group.date.month.number, group.date.day) },
                            Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(if (group.totalAmountOverflow) stringResource(Res.string.expense_total_overflow)
                            else stringResource(Res.string.expense_amount_won, group.totalAmountText),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
                items(group.expenses, key = { "expense:${it.id}" }, contentType = { "expense-row" }) { expense ->
                    Box(Modifier.padding(horizontal = 24.dp)) {
                        ExpenseRow(expense) { onIntent(ExpenseListUiIntent.OpenExpense(expense.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyContent(unavailable: Boolean, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(stringResource(if (unavailable) Res.string.expense_search_unavailable_title else Res.string.expense_search_empty_title),
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Text(stringResource(if (unavailable) Res.string.expense_search_unavailable_description else Res.string.expense_search_empty_description),
            Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        TextButton(onClick = onClear) { Text(stringResource(Res.string.expense_search_clear)) }
    }
}

@Composable
private fun ExpenseHeader(
    state: ExpenseListUiState,
    onIntent: (ExpenseListUiIntent) -> Unit,
    onToggleSearch: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(if (state.isSearchOpen) Res.string.expense_search_open else Res.string.expense_list_title), Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onToggleSearch) {
                Icon(painterResource(if (state.isSearchOpen) Res.drawable.ic_close else Res.drawable.ic_search),
                    stringResource(if (state.isSearchOpen) Res.string.expense_search_close else Res.string.expense_search_open))
            }
        }
        if (!state.isSearchOpen) {
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
        }
        if (!state.isSearchOpen) {
            Text(
                text = stringResource(Res.string.expense_month_total, state.month.month.number),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            val currency = stringResource(Res.string.expense_currency_won)
            if (state.totalAmountOverflow) Text(stringResource(Res.string.expense_total_overflow),
                style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp))
            else Text(
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
                    expense.paymentMethod?.let { paymentLabel(it) } ?: expense.paymentMethodName,
                ),
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            expense.memo?.takeIf { it.isNotBlank() }?.let { memo ->
                Text(memo, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
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
