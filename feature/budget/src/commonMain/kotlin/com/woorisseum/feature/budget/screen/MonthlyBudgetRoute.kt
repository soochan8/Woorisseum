package com.woorisseum.feature.budget.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.woorisseum.feature.budget.model.MonthlyBudgetUiEffect
import com.woorisseum.feature.budget.viewmodel.MonthlyBudgetViewModel
import kotlinx.datetime.YearMonth
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** DI·생명주기 기반 상태 수집·이동 콜백을 연결한다. */
@Composable
fun MonthlyBudgetRoute(
    initialMonth: YearMonth,
    onBack: () -> Unit,
    onViewExpenses: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MonthlyBudgetViewModel = koinViewModel(parameters = { parametersOf(initialMonth) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnViewExpenses by rememberUpdatedState(onViewExpenses)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is MonthlyBudgetUiEffect.NavigateToExpenses -> currentOnViewExpenses(effect.month)
                }
            }
        }
    }
    MonthlyBudgetScreen(state, viewModel::onIntent, onBack, modifier)
}
