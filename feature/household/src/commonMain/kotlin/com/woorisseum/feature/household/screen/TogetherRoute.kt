package com.woorisseum.feature.household.screen

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
fun TogetherRoute(
    onBack: () -> Unit,
    incomingId: Long? = null,
    incomingCode: String? = null,
    onIncomingHandled: (Long) -> Unit = {},
    onShareInvitation: (String) -> Unit = {},
    viewModel: TogetherViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(incomingId, state.isLoading, state.isSaving, state.snapshot) {
        if (incomingId != null && incomingCode != null && state.snapshot != null && !state.isLoading && !state.isSaving) {
            viewModel.onIntent(TogetherUiIntent.OpenInvitation(incomingCode))
            onIncomingHandled(incomingId)
        }
    }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var codeCopied by remember(state.issued?.id) { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        while (true) { delay(1000); viewModel.onIntent(TogetherUiIntent.Tick) }
    }
    val focus = LocalFocusManager.current
    val back: () -> Unit = {
        if (!state.isSaving) {
        focus.clearFocus()
        if (state.step == TogetherStep.Start || state.step == TogetherStep.Connected) onBack() else viewModel.onIntent(TogetherUiIntent.Back)
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.step != TogetherStep.Start && state.step != TogetherStep.Connected,
        onBackCompleted = back,
    )
    TogetherScreen(state = state, onIntent = viewModel::onIntent, onBack = back, codeCopied = codeCopied, onShareCode = { code ->
        val issued = state.issued
        if (issued != null && issued.code.value == code && kotlin.time.Clock.System.now() < issued.expiresAt && !state.isSaving && !state.isLoading) onShareInvitation(code)
    }, onCopyCode = { code -> scope.launch { clipboard.setClipEntry(invitationClipEntry(code)); codeCopied = true } })
}
