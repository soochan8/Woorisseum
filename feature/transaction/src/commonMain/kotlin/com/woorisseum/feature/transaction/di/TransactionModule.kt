package com.woorisseum.feature.transaction.di

import com.woorisseum.feature.transaction.viewmodel.ExpenseDetailViewModel
import com.woorisseum.feature.transaction.viewmodel.ExpenseEditViewModel
import com.woorisseum.feature.transaction.fixed.FixedExpenseListViewModel
import com.woorisseum.feature.transaction.fixed.FixedExpenseEditorViewModel
import com.woorisseum.feature.transaction.viewmodel.ExpenseListViewModel
import com.woorisseum.feature.transaction.viewmodel.ExpenseEntryViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Clock

/** 기능의 객체 생성 방법을 정의하고, 앱 전체 조립은 shared에 맡긴다. */
val transactionModule = module {
    viewModel { parameters -> FixedExpenseListViewModel(initialMonth = parameters.get<YearMonth>(), calculateMonthly = get()) }
    viewModel { parameters -> FixedExpenseEditorViewModel(initialMonth = parameters.get<YearMonth>(), planId = parameters.getOrNull<String>()) }
    viewModel { parameters -> ExpenseDetailViewModel(expenseId = parameters.get<String>()) }
    viewModel { parameters -> ExpenseEditViewModel(expenseId = parameters.get<String>()) }
    viewModel {
        ExpenseEntryViewModel(initialDate = Clock.System.todayIn(TimeZone.currentSystemDefault()))
    }
    viewModel { parameters ->
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        ExpenseListViewModel(initialMonth = parameters.getOrNull<YearMonth>() ?: YearMonth(today.year, today.month))
    }
}
