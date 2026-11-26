package com.woorisseum.di

import android.content.Context
import com.woorisseum.config.GeneratedSupabaseConfig
import com.woorisseum.core.data.auth.sessionStorageModule
import com.woorisseum.core.data.di.databaseModule

/** Application에서 한 번 호출해 화면 재생성과 독립적인 DI 수명을 갖는다. */
fun initializeAndroidApp(context: Context) {
    startAppKoin(listOf(databaseModule(context), sessionStorageModule(context, GeneratedSupabaseConfig.url)))
}
