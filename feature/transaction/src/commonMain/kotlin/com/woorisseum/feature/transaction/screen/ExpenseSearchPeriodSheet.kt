package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.woorisseum.feature.transaction.model.ExpenseSearchPeriod
import com.woorisseum.feature.transaction.model.ExpenseSearchPreset
import com.woorisseum.feature.transaction.resources.*
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

/** 빠른 선택은 즉시 적용하고, 직접 선택은 별도 달력에서 확인한 뒤 적용한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseSearchPeriodSheet(
    selected: ExpenseSearchPeriod, onSelect: (ExpenseSearchPreset) -> Unit, onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).selectableGroup()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text(stringResource(Res.string.expense_search_period), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp))
            ExpenseSearchPreset.entries.forEach { preset ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .selectable(selected.preset == preset, role = Role.RadioButton, onClick = { onSelect(preset) }),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected.preset == preset, onClick = null)
                    Column {
                        Text(searchPresetLabel(preset))
                        if (preset == ExpenseSearchPreset.CUSTOM && selected.range != null) {
                            Text(searchPeriodRangeLabel(selected), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun searchPresetLabel(preset: ExpenseSearchPreset): String = stringResource(when (preset) {
    ExpenseSearchPreset.ALL -> Res.string.expense_search_period_all
    ExpenseSearchPreset.THIS_MONTH -> Res.string.expense_search_period_month
    ExpenseSearchPreset.RECENT_THREE_MONTHS -> Res.string.expense_search_period_three_months
    ExpenseSearchPreset.THIS_YEAR -> Res.string.expense_search_period_year
    ExpenseSearchPreset.CUSTOM -> Res.string.expense_search_period_custom
})

@Composable
internal fun searchPeriodRangeLabel(period: ExpenseSearchPeriod): String = period.range?.let {
    stringResource(Res.string.expense_search_period_range, it.start.year, it.start.month.number, it.start.day,
        it.endInclusive.year, it.endInclusive.month.number, it.endInclusive.day)
} ?: stringResource(Res.string.expense_search_period_all)
