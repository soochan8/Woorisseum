package com.woorisseum

import androidx.compose.ui.window.ComposeUIViewController
import com.woorisseum.core.data.di.databaseModule
import com.woorisseum.di.startAppKoin

// 호스트 컨트롤러를 다시 생성해도 DI·DB 인스턴스를 재생성하지 않는다.
private val appKoin by lazy { startAppKoin(databaseModule()) }

fun MainViewController(): platform.UIKit.UIViewController {
    appKoin
    return ComposeUIViewController { App() }
}
