package com.woorisseum

import androidx.compose.runtime.Composable
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.di.appConfiguration
import com.woorisseum.navigation.AppNavigation
import org.koin.compose.KoinApplication

/** 두 플랫폼이 공유하는 DI·테마·내비게이션 진입점이다. */
@Composable
fun App() {
    KoinApplication(configuration = appConfiguration) {
        WoorisseumTheme {
            AppNavigation()
        }
    }
}
