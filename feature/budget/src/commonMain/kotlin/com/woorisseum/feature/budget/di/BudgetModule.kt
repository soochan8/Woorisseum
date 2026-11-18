package com.woorisseum.feature.budget.di

import com.woorisseum.feature.budget.viewmodel.MonthlyBudgetViewModel
import kotlinx.datetime.YearMonth
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val budgetModule = module {
    viewModel { parameters ->
        MonthlyBudgetViewModel(initialMonth = parameters.get<YearMonth>(), calculateSummary = get())
    }
}
