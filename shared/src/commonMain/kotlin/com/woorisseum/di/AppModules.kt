package com.woorisseum.di

import com.woorisseum.feature.transaction.di.transactionModule
import org.koin.dsl.koinConfiguration

/** 앱에서 사용하는 기능·데이터 DI 정의를 모으는 조립 지점이다. */
internal val appConfiguration = koinConfiguration {
    modules(transactionModule)
}
