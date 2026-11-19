package com.woorisseum.feature.transaction.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woorisseum.feature.transaction.viewmodel.ExpenseEntryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** ViewModel의 생명주기·상태 수집을 연결하고 앱의 뒤로 이동 콜백을 전달한다. */
@Composable
fun ExpenseEntryRoute(
    onBack: () -> Unit,
    onOpenCards: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseEntryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseEntryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onOpenCards = onOpenCards,
        modifier = modifier,
    )
}
