package com.woorisseum.core.data.di

import com.woorisseum.core.data.transaction.card.InMemoryCardRepository
import com.woorisseum.core.domain.transaction.card.CardRepository
import org.koin.dsl.module

val dataModule = module {
    single<CardRepository> { InMemoryCardRepository() }
}
