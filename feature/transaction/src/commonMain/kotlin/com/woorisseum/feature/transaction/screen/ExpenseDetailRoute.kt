package com.woorisseum.feature.transaction.screen

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.woorisseum.feature.transaction.model.ExpenseDetailUiEffect
import com.woorisseum.feature.transaction.viewmodel.ExpenseDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** ID 기반 DI·생명주기 상태 수집과 앱의 이동 콜백을 연결한다. */
@Composable
fun ExpenseDetailRoute(
    expenseId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseDetailViewModel = koinViewModel(parameters = { parametersOf(expenseId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEdit by rememberUpdatedState(onEdit)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ExpenseDetailUiEffect.NavigateToEdit -> currentOnEdit(effect.expenseId)
                }
            }
        }
    }
    ExpenseDetailScreen(state, viewModel::onIntent, onBack, modifier)
}
