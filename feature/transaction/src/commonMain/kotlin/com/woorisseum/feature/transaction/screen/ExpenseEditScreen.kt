package com.woorisseum.feature.transaction.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.woorisseum.feature.transaction.model.*
import com.woorisseum.feature.transaction.resources.*
import org.jetbrains.compose.resources.stringResource

/** 등록과 같은 폼을 사용하며, 수정 전용 제출·취소 의도를 전달한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditScreen(
    state: ExpenseEditUiState,
    onIntent: (ExpenseEditUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.expense_edit_title)) },
                navigationIcon = {
                    TextButton(onClick = { onIntent(ExpenseEditUiIntent.Cancel) }) {
                        Text(stringResource(Res.string.expense_cancel))
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    enabled = state.form != null && state.hasChanges,
                    onClick = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                        onIntent(ExpenseEditUiIntent.Submit)
                    },
                    modifier = Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) { Text(stringResource(Res.string.expense_edit_submit)) }
            }
        },
    ) { padding ->
        val form = state.form
        if (form == null) {
            ExpenseLookupUnavailable(Modifier.fillMaxSize().padding(padding))
        } else {
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ExpenseForm(form, { onIntent(ExpenseEditUiIntent.FormChanged(it)) })
                Text(stringResource(if (state.showStorageUnavailable)
                    Res.string.expense_edit_storage_unavailable else Res.string.expense_edit_notice),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
