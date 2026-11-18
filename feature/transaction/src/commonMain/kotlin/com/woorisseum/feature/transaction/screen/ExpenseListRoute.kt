package com.woorisseum.feature.transaction.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.woorisseum.feature.transaction.model.ExpenseListUiEffect
import com.woorisseum.feature.transaction.viewmodel.ExpenseListViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.datetime.YearMonth

/** DI·상태 수집·이동 요청을 연결한다. Screen은 ViewModel을 알지 못한다. */
@Composable
fun ExpenseListRoute(
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onOpenBudget: (YearMonth) -> Unit,
    initialMonth: YearMonth? = null,
    modifier: Modifier = Modifier,
    viewModel: ExpenseListViewModel = koinViewModel(parameters = { parametersOf(initialMonth) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnOpenExpense by rememberUpdatedState(onOpenExpense)
    val currentOnAddExpense by rememberUpdatedState(onAddExpense)
    val currentOnOpenBudget by rememberUpdatedState(onOpenBudget)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ExpenseListUiEffect.NavigateToDetail -> currentOnOpenExpense(effect.expenseId)
                    ExpenseListUiEffect.NavigateToAddExpense -> currentOnAddExpense()
                    is ExpenseListUiEffect.NavigateToBudget -> currentOnOpenBudget(effect.month)
                }
            }
        }
    }

    ExpenseListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
