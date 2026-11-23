package com.woorisseum.core.data.di

import com.woorisseum.core.data.local.WoorisseumDatabase
import com.woorisseum.core.data.budget.LocalBudgetRepository
import com.woorisseum.core.domain.budget.repository.BudgetRepository
import com.woorisseum.core.data.transaction.LocalExpenseRepository
import com.woorisseum.core.domain.transaction.repository.ExpenseRepository
import com.woorisseum.core.data.transaction.card.InMemoryCardRepository
import com.woorisseum.core.data.transaction.card.LocalCardRepository
import com.woorisseum.core.domain.transaction.card.CardRepository
import org.koin.dsl.module
import com.woorisseum.core.data.transaction.fixed.LocalFixedExpenseRepository
import com.woorisseum.core.domain.transaction.fixed.FixedExpenseRepository

val dataModule = module {
    single<FixedExpenseRepository> { LocalFixedExpenseRepository(get()) }
    single<BudgetRepository> { LocalBudgetRepository(get<WoorisseumDatabase>().budgetDao()) }
    single<ExpenseRepository> { LocalExpenseRepository(get()) }
    single<CardRepository> { LocalCardRepository(get<WoorisseumDatabase>().cardDao()) }
}

/** IDE Preview에서만 쓰는 별도 메모리 저장소다. 운영 바인딩을 대체하지 않는다. */
val previewDataModule = module {
    single<CardRepository> { InMemoryCardRepository() }
}
