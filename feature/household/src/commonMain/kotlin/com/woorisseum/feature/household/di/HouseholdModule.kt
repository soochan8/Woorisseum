package com.woorisseum.feature.household.di

import com.woorisseum.feature.household.viewmodel.TogetherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val householdModule = module {
    viewModel { TogetherViewModel(sessionRepository = get()) }
}
