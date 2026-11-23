package com.woorisseum.feature.shopping.di

import com.woorisseum.feature.shopping.viewmodel.ShoppingListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 초기 예시 데이터·가짜 접속자를 주입하지 않는다. */
val shoppingModule = module { viewModel { ShoppingListViewModel(get()) } }
