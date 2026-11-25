package com.woorisseum.feature.transaction.screen

import com.woorisseum.core.designsystem.theme.WoorisseumSpacing
import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import com.woorisseum.core.designsystem.component.WoorisseumFilterChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.feature.transaction.model.ExpenseDateRange
import com.woorisseum.feature.transaction.model.ExpenseSearchPreset
import com.woorisseum.feature.transaction.model.ExpenseListUiIntent
import com.woorisseum.feature.transaction.model.ExpenseListUiState
import com.woorisseum.feature.transaction.model.toExpensePaymentFilter
import com.woorisseum.feature.transaction.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

/** 필터 입력 UI다. 조건의 변경·검증은 ViewModel에 맡긴다. */
@Composable
internal fun ExpenseListFilterBar(state: ExpenseListUiState, onIntent: (ExpenseListUiIntent) -> Unit) {
    var showDates by rememberSaveable(state.month.toString(), state.isSearchOpen) { mutableStateOf(false) }
    var showSearchPeriods by rememberSaveable(state.isSearchOpen) { mutableStateOf(false) }
    var showCustomSearchPeriod by rememberSaveable(state.isSearchOpen) { mutableStateOf(false) }
    var showPayments by rememberSaveable { mutableStateOf(false) }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = WoorisseumSpacing.screenHorizontal, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WoorisseumFilterChip(
            selected = if (state.isSearchOpen) state.searchPeriod.preset != ExpenseSearchPreset.ALL else state.filters.dateRange != null,
            onClick = { if (state.isSearchOpen) showSearchPeriods = true else showDates = true },
            label = {
                if (state.isSearchOpen) Text(
                    if (state.searchPeriod.preset == ExpenseSearchPreset.CUSTOM) searchPeriodRangeLabel(state.searchPeriod)
                    else searchPresetLabel(state.searchPeriod.preset), modifier = Modifier.widthIn(max = 250.dp),
                ) else Text(state.filters.dateRange?.let {
                    stringResource(Res.string.expense_filter_date_range, it.start.month.number, it.start.day,
                        it.endInclusive.month.number, it.endInclusive.day)
                } ?: stringResource(Res.string.expense_filter_date_all))
            },
        )
        WoorisseumFilterChip(
            selected = state.filters.paymentMethod != null,
            onClick = { showPayments = true },
            label = {
                val selected = state.availablePaymentMethods.firstOrNull {
                    it.toExpensePaymentFilter() == state.filters.paymentMethod
                }
                Text(
                    selected?.let { paymentLabel(it) } ?: stringResource(Res.string.expense_filter_payment_all),
                    modifier = Modifier.widthIn(max = 160.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            },
        )
        val hasFilters = state.filters.category != null || state.filters.paymentMethod != null ||
            if (state.isSearchOpen) state.searchPeriod.preset != ExpenseSearchPreset.ALL else state.filters.dateRange != null
        if (hasFilters) {
            WoorisseumTextButton(onClick = { onIntent(ExpenseListUiIntent.ResetFilters) }) {
                Text(stringResource(Res.string.expense_filter_reset))
            }
        }
    }
    if (showSearchPeriods) {
        ExpenseSearchPeriodSheet(state.searchPeriod, onSelect = { preset ->
            showSearchPeriods = false
            if (preset == ExpenseSearchPreset.CUSTOM) showCustomSearchPeriod = true
            else onIntent(ExpenseListUiIntent.SelectSearchPreset(preset))
        }, onDismiss = { showSearchPeriods = false })
    }
    if (showCustomSearchPeriod) {
        ExpenseDateRangeDialog(month = state.month, selectedRange = state.searchPeriod.range,
            restrictToMonth = false,
            onDismiss = { showCustomSearchPeriod = false; showSearchPeriods = true },
            onSelect = { start, end ->
                onIntent(ExpenseListUiIntent.SelectSearchRange(start, end))
                showCustomSearchPeriod = false
            })
    }
    if (showDates) {
        ExpenseDateRangeDialog(
            month = state.month,
            selectedRange = state.filters.dateRange,
            onDismiss = { showDates = false },
            onSelect = { start, end ->
                onIntent(ExpenseListUiIntent.SelectDateRange(start, end))
                showDates = false
            },
        )
    }
    if (showPayments) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showPayments = false },
            title = { Text(stringResource(Res.string.expense_payment_label)) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()).selectableGroup()) {
                    // 선택지 목록을 확장하면 카드도 같은 UI에서 표시한다. 카드명은 표시 용도로만 사용한다.
                    (listOf(null) + state.availablePaymentMethods).forEach { payment ->
                        val filter = payment?.toExpensePaymentFilter()
                        val selected = filter == state.filters.paymentMethod
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected, role = Role.RadioButton, onClick = {
                                    onIntent(ExpenseListUiIntent.SelectPaymentMethod(filter))
                                    showPayments = false
                                }).padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(payment?.let { paymentLabel(it) }
                                ?: stringResource(Res.string.expense_filter_payment_all))
                        }
                    }
                }
            },
            confirmButton = {
                WoorisseumTextButton(onClick = { showPayments = false }) { Text(stringResource(Res.string.expense_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDateRangeDialog(
    month: YearMonth,
    selectedRange: ExpenseDateRange?,
    onDismiss: () -> Unit,
    onSelect: (LocalDate, LocalDate) -> Unit,
    restrictToMonth: Boolean = true,
) {
    val selectableDates = remember(month, restrictToMonth) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = LocalDate.fromEpochDays(utcTimeMillis / 86_400_000L)
                return !restrictToMonth || date >= month.firstDay && date <= month.lastDay
            }
        }
    }
    val picker = rememberDateRangePickerState(
        initialSelectedStartDateMillis = selectedRange?.start?.toEpochDays()?.times(86_400_000L),
        initialSelectedEndDateMillis = selectedRange?.endInclusive?.toEpochDays()?.times(86_400_000L),
        initialDisplayedMonthMillis = (selectedRange?.start ?: month.firstDay).toEpochDays() * 86_400_000L,
        yearRange = if (restrictToMonth) month.year..month.year else
            minOf(1900, month.year, selectedRange?.start?.year ?: month.year)..maxOf(2100, month.year, selectedRange?.endInclusive?.year ?: month.year),
        selectableDates = selectableDates,
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.safeDrawingPadding().imePadding().padding(16.dp).widthIn(max = 560.dp)
                .fillMaxWidth().heightIn(max = if (picker.displayMode == DisplayMode.Input) 360.dp else 600.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                Text(
                    if (restrictToMonth) stringResource(Res.string.expense_filter_date_title, month.year, month.month.number)
                    else stringResource(Res.string.expense_search_period_custom_title),
                    modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium,
                )
                DateRangePicker(
                    state = picker, modifier = Modifier.weight(1f), title = null, headline = null,
                    showModeToggle = !restrictToMonth,
                    colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    if (restrictToMonth) WoorisseumTextButton(onClick = { onSelect(month.firstDay, month.lastDay) }) {
                        Text(stringResource(Res.string.expense_filter_date_all))
                    }
                    WoorisseumTextButton(onClick = onDismiss) { Text(stringResource(Res.string.expense_cancel)) }
                    WoorisseumTextButton(
                        enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null,
                        onClick = {
                            val start = picker.selectedStartDateMillis
                            val end = picker.selectedEndDateMillis
                            if (start != null && end != null) onSelect(
                                LocalDate.fromEpochDays(start / 86_400_000L),
                                LocalDate.fromEpochDays(end / 86_400_000L),
                            )
                        },
                    ) { Text(stringResource(Res.string.expense_confirm)) }
                }
            }
        }
    }
}
