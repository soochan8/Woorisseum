package com.woorisseum.core.data.di

import com.woorisseum.core.data.local.WoorisseumDatabase
import com.woorisseum.core.data.transaction.LocalExpenseRepository
import com.woorisseum.core.domain.transaction.repository.ExpenseRepository
import com.woorisseum.core.data.transaction.card.InMemoryCardRepository
import com.woorisseum.core.data.transaction.card.LocalCardRepository
import com.woorisseum.core.domain.transaction.card.CardRepository
import org.koin.dsl.module

val dataModule = module {
    single<ExpenseRepository> { LocalExpenseRepository(get()) }
    single<CardRepository> { LocalCardRepository(get<WoorisseumDatabase>().cardDao()) }
}

/** IDE Preview에서만 쓰는 별도 메모리 저장소다. 운영 바인딩을 대체하지 않는다. */
val previewDataModule = module {
    single<CardRepository> { InMemoryCardRepository() }
}
