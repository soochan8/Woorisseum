package com.woorisseum.core.data.local

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** 고정지출 이력을 추가하며 기존 카드·지출·예산을 보존한다. */
@Database(
    entities = [CardEntity::class, ExpenseEntity::class, BudgetEntity::class, FixedExpenseEntity::class, FixedExpenseVersionEntity::class],
    version = 3, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@ConstructedBy(WoorisseumDatabaseConstructor::class)
internal abstract class WoorisseumDatabase : RoomDatabase() {
    abstract fun fixedExpenseDao(): FixedExpenseDao
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
