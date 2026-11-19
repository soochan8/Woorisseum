package com.woorisseum.di

import com.woorisseum.feature.transaction.di.transactionModule
import com.woorisseum.feature.budget.di.budgetModule
import com.woorisseum.core.domain.transaction.fixed.CalculateMonthlyFixedExpensesUseCase
import com.woorisseum.core.domain.budget.usecase.CalculateBudgetSummaryUseCase
import com.woorisseum.feature.shopping.di.shoppingModule
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

private val domainModule = module {
    factory { CalculateMonthlyFixedExpensesUseCase() }
    factory { CalculateBudgetSummaryUseCase() }
}

/** 앱에서 사용하는 기능·데이터 DI 정의를 모으는 조립 지점이다. */
internal val appConfiguration = koinConfiguration {
    modules(domainModule, transactionModule, budgetModule, shoppingModule)
}
