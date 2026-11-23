package com.woorisseum.feature.transaction.fixed

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.datetime.YearMonth
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/** 월·ID만 앱의 화면 이동 경계로 전달한다. */
@Composable
fun FixedExpenseListRoute(
    initialMonth: YearMonth,
    onBack: () -> Unit,
    onOpenEditor: (YearMonth, String?) -> Unit,
    requestedMonth: YearMonth? = null,
    onMonthHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FixedExpenseListViewModel = koinViewModel(parameters = { parametersOf(initialMonth) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val currentOpen by rememberUpdatedState(onOpenEditor)
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effects.collect { effect ->
                when (effect) { is FixedExpenseListUiEffect.OpenEditor -> currentOpen(effect.month, effect.planId) }
            }
        }
    }
    LaunchedEffect(viewModel, requestedMonth) {
        requestedMonth?.let { viewModel.onIntent(FixedExpenseListUiIntent.ShowMonth(it)); onMonthHandled() }
    }
    FixedExpenseListScreen(state, viewModel::onIntent, onBack, modifier)
}

@Composable
fun FixedExpenseEditorRoute(
    initialMonth: YearMonth,
    onBack: () -> Unit,
    planId: String? = null,
    onSaved: (YearMonth) -> Unit = { onBack() },
    modifier: Modifier = Modifier,
    viewModel: FixedExpenseEditorViewModel = koinViewModel(parameters = { parametersOf(initialMonth, planId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val currentBack by rememberUpdatedState(onBack)
    val currentSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch {
                viewModel.uiState.map { if (it.saved) it.form?.startMonth else null }.distinctUntilChanged().collect { month ->
                    if (month != null) currentSaved(month)
                }
            }
            viewModel.effects.collect { effect ->
                when (effect) { FixedExpenseEditorUiEffect.NavigateBack -> currentBack() }
            }
        }
    }
    FixedExpenseEditorScreen(state, viewModel::onIntent, modifier)
}
