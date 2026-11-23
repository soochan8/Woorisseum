package com.woorisseum.feature.transaction.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woorisseum.feature.transaction.viewmodel.ExpenseEntryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** ViewModel의 생명주기·상태 수집을 연결하고 앱의 뒤로 이동 콜백을 전달한다. */
@Composable
fun ExpenseEntryRoute(
    onBack: () -> Unit,
    onOpenCards: () -> Unit,
    onSaved: (LocalDate) -> Unit = { onBack() },
    modifier: Modifier = Modifier,
    viewModel: ExpenseEntryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.uiState.map { it.form.date.takeIf { _ -> it.saved } }.distinctUntilChanged().collect { date ->
                if (date != null) currentOnSaved(date)
            }
        }
    }
    ExpenseEntryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onOpenCards = onOpenCards,
        modifier = modifier,
    )
}
