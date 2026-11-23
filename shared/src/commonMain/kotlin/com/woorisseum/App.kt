package com.woorisseum

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.feature.transaction.screen.ExpenseListScreen
import com.woorisseum.feature.transaction.model.ExpenseListUiState
import kotlinx.datetime.YearMonth
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import com.woorisseum.navigation.AppNavigation

/** 런타임 DI는 플랫폼 진입점에서 초기화하고 Preview는 상태만 렌더링한다. */
@Composable
fun App() {
    if (LocalInspectionMode.current) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        WoorisseumTheme {
            ExpenseListScreen(ExpenseListUiState(month = YearMonth(today.year, today.month),
                isExpenseSourceReady = true), {})
        }
    } else {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    WoorisseumTheme { AppNavigation() }
}
