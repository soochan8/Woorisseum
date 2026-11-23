package com.woorisseum.feature.transaction.screen

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.woorisseum.feature.transaction.model.ExpenseEditUiEffect
import com.woorisseum.feature.transaction.viewmodel.ExpenseEditViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/** ID 기반 DI·생명주기 상태 수집과 앱의 이동 콜백을 연결한다. */
@Composable
fun ExpenseEditRoute(
    expenseId: String,
    onBack: () -> Unit,
    onOpenCards: () -> Unit,
    onSaved: (LocalDate) -> Unit = { onBack() },
    modifier: Modifier = Modifier,
    viewModel: ExpenseEditViewModel = koinViewModel(parameters = { parametersOf(expenseId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch {
                viewModel.uiState.map { if (it.saved) it.form?.date else null }.distinctUntilChanged().collect { date ->
                    if (date != null) currentOnSaved(date)
                }
            }
            viewModel.effects.collect { effect ->
                when (effect) {
                    ExpenseEditUiEffect.NavigateBack -> currentOnBack()
                }
            }
        }
    }
    ExpenseEditScreen(state, viewModel::onIntent, modifier, onOpenCards = onOpenCards)
}
