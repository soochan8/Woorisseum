package com.woorisseum.di

import com.woorisseum.config.GeneratedSupabaseConfig
import com.woorisseum.core.data.auth.SupabaseConfig
import com.woorisseum.core.data.auth.remoteModule
import com.woorisseum.feature.transaction.di.transactionModule
import com.woorisseum.feature.household.di.householdModule
import com.woorisseum.feature.budget.di.budgetModule
import com.woorisseum.core.domain.transaction.fixed.CalculateMonthlyFixedExpensesUseCase
import com.woorisseum.core.domain.budget.usecase.CalculateBudgetSummaryUseCase
import com.woorisseum.feature.shopping.di.shoppingModule
import com.woorisseum.core.data.di.dataModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

private val domainModule = module {
    factory { CalculateMonthlyFixedExpensesUseCase() }
    factory { CalculateBudgetSummaryUseCase() }
}

/** 앱에서 사용하는 기능·데이터 DI 정의를 모으는 조립 지점이다. */
private val commonModules = listOf(domainModule, transactionModule, budgetModule, shoppingModule, householdModule)

internal fun startAppKoin(platformModules: List<Module>) = startKoin {
    modules(commonModules + platformModules + listOf(dataModule,
        remoteModule(SupabaseConfig(GeneratedSupabaseConfig.url, GeneratedSupabaseConfig.publishableKey))))
}
