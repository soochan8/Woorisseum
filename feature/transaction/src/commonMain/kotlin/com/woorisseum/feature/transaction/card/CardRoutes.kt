package com.woorisseum.feature.transaction.card

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CardListRoute(
    onBack: () -> Unit,
    onOpenEditor: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val currentOpen by rememberUpdatedState(onOpenEditor)
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) { is CardListUiEffect.OpenEditor -> currentOpen(effect.cardId) }
            }
        }
    }
    CardListScreen(state, viewModel::onIntent, onBack, modifier)
}

@Composable
fun CardEditorRoute(
    onBack: () -> Unit,
    cardId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: CardEditorViewModel = koinViewModel(parameters = { parametersOf(cardId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val currentBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { if (it.saved) currentBack() }
        }
    }
    CardEditorScreen(state, viewModel::onIntent, onBack, modifier)
}
