package com.woorisseum.feature.transaction.di

import com.woorisseum.feature.transaction.card.CardEditorViewModel
import com.woorisseum.feature.transaction.card.CardListViewModel
import com.woorisseum.feature.transaction.fixed.FixedExpenseEditorViewModel
import com.woorisseum.feature.transaction.fixed.FixedExpenseListViewModel
import com.woorisseum.feature.transaction.viewmodel.ExpenseDetailViewModel
import com.woorisseum.feature.transaction.viewmodel.ExpenseEditViewModel
import com.woorisseum.feature.transaction.viewmodel.ExpenseEntryViewModel
import com.woorisseum.feature.transaction.viewmodel.ExpenseListViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 기능의 객체 생성 방법을 정의하고, 앱 전체 조립은 shared에 맡긴다. */
val transactionModule = module {
    viewModel { CardListViewModel(repository = get()) }
    viewModel { parameters -> CardEditorViewModel(repository = get(), cardId = parameters.getOrNull<String>()) }
    viewModel { parameters -> FixedExpenseListViewModel(initialMonth = parameters.get<YearMonth>(), calculateMonthly = get(), repository = get()) }
    viewModel { parameters -> FixedExpenseEditorViewModel(initialMonth = parameters.get<YearMonth>(), planId = parameters.getOrNull<String>(), repository = get()) }
    viewModel { parameters -> ExpenseDetailViewModel(expenseId = parameters.get<String>(), repository = get()) }
    viewModel { parameters -> ExpenseEditViewModel(expenseId = parameters.get<String>(), cardRepository = get(), repository = get()) }
    viewModel {
        ExpenseEntryViewModel(initialDate = Clock.System.todayIn(TimeZone.currentSystemDefault()), cardRepository = get(), repository = get())
    }
    viewModel { parameters ->
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        ExpenseListViewModel(initialMonth = parameters.getOrNull<YearMonth>() ?: YearMonth(today.year, today.month), cardRepository = get(), repository = get())
    }
}
