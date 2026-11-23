package com.woorisseum

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.di.previewConfiguration
import com.woorisseum.navigation.AppNavigation
import org.koin.compose.KoinApplication

/** 런타임 DI는 플랫폼 진입점에서 초기화하고 Preview만 별도 컨테이너를 사용한다. */
@Composable
fun App() {
    if (LocalInspectionMode.current) {
        KoinApplication(configuration = previewConfiguration) { AppContent() }
    } else {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    WoorisseumTheme { AppNavigation() }
}
