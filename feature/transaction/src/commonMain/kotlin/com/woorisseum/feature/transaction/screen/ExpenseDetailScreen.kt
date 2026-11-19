package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.feature.transaction.model.*
import com.woorisseum.feature.transaction.resources.*
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

/** 표시값과 콜백만 사용한다. 조회·수정 동작은 ViewModel과 Route에 맡긴다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    state: ExpenseDetailUiState,
    onIntent: (ExpenseDetailUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.expense_detail_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(Res.string.expense_back)) }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = { onIntent(ExpenseDetailUiIntent.Edit) },
                    enabled = state.detail != null,
                    modifier = Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) { Text(stringResource(Res.string.expense_edit_action)) }
            }
        },
    ) { padding ->
        val detail = state.detail
        if (detail == null) {
            ExpenseLookupUnavailable(Modifier.fillMaxSize().padding(padding))
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(stringResource(Res.string.expense_amount_won, detail.amountText),
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text(detail.merchant ?: stringResource(Res.string.expense_unknown_merchant),
                    style = MaterialTheme.typography.titleMedium)
                DetailRow(stringResource(Res.string.expense_date_label),
                    stringResource(Res.string.expense_date_value, detail.date.year, detail.date.month.number, detail.date.day))
                DetailRow(stringResource(Res.string.expense_category_label), stringResource(when (detail.category) {
                    ExpenseCategory.FIXED -> Res.string.expense_category_fixed
                    ExpenseCategory.VARIABLE -> Res.string.expense_category_variable
                    ExpenseCategory.IMPULSE -> Res.string.expense_category_impulse
                }))
                DetailRow(stringResource(Res.string.expense_payment_label), paymentLabel(detail.paymentMethod))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.expense_memo_title),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Text(detail.memo ?: stringResource(Res.string.expense_memo_empty),
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, Modifier.weight(2f), textAlign = TextAlign.End)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** 미연결 상태를 빈 거래나 조회 실패로 바꾸지 않는다. */
@Composable
internal fun ExpenseLookupUnavailable(modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.expense_lookup_unavailable_title),
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.expense_lookup_unavailable_description),
            Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
