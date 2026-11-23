package com.woorisseum.core.data.local

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** 월 예산을 추가하며 기존 카드·지출은 자동 마이그레이션으로 보존한다. */
@Database(
    entities = [CardEntity::class, ExpenseEntity::class, BudgetEntity::class],
    version = 2, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(WoorisseumDatabaseConstructor::class)
internal abstract class WoorisseumDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    abstract fun cardDao(): CardDao
    abstract fun expenseDao(): ExpenseDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object WoorisseumDatabaseConstructor : RoomDatabaseConstructor<WoorisseumDatabase> {
    override fun initialize(): WoorisseumDatabase
}

internal const val DATABASE_NAME = "woorisseum.db"

internal fun buildDatabase(builder: RoomDatabase.Builder<WoorisseumDatabase>): WoorisseumDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
