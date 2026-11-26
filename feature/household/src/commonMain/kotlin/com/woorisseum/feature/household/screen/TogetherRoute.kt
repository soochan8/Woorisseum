package com.woorisseum.feature.household.screen

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.woorisseum.feature.household.model.*
import com.woorisseum.feature.household.viewmodel.TogetherViewModel
import org.koin.compose.viewmodel.koinViewModel

/** 기능 내 단계는 상태로, 앱 화면 이동은 shared의 콜백으로 처리한다. */
@Composable
fun TogetherRoute(onBack: () -> Unit, viewModel: TogetherViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val back: () -> Unit = {
        focus.clearFocus()
        if (state.step == TogetherStep.Start || state.step == TogetherStep.Connected) onBack() else viewModel.onIntent(TogetherUiIntent.Back)
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.step != TogetherStep.Start && state.step != TogetherStep.Connected,
        onBackCompleted = back,
    )
    TogetherScreen(state = state, onIntent = viewModel::onIntent, onBack = back)
}
