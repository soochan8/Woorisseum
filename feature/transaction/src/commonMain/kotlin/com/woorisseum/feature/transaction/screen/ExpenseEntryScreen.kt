package com.woorisseum.feature.transaction.screen

import com.woorisseum.core.designsystem.theme.WoorisseumSpacing
import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.component.WoorisseumBottomActionBar
import com.woorisseum.core.designsystem.component.WoorisseumPrimaryButton
import com.woorisseum.feature.transaction.model.ExpenseEntryUiIntent
import com.woorisseum.feature.transaction.model.ExpenseEntryUiState
import com.woorisseum.feature.transaction.resources.*
import org.jetbrains.compose.resources.stringResource

/** 입력값을 상태로 받고, 사용자 동작은 Intent로 전달한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryScreen(
    state: ExpenseEntryUiState,
    onIntent: (ExpenseEntryUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCards: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.expense_entry_title)) },
                navigationIcon = {
                    WoorisseumTextButton(onClick = onBack) { Text(stringResource(Res.string.expense_back)) }
                },
            )
        },
        bottomBar = {
            WoorisseumBottomActionBar {
                WoorisseumPrimaryButton(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onIntent(ExpenseEntryUiIntent.Submit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.expense_save)) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = WoorisseumSpacing.screenHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ExpenseForm(state.form, { onIntent(ExpenseEntryUiIntent.FormChanged(it)) }, onOpenCards = onOpenCards)
            if (state.showStorageUnavailable) {
                Text(
                    stringResource(Res.string.expense_storage_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
