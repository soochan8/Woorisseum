package com.woorisseum.core.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** 카드·지출의 초기 스키마. 기능별 쓰기는 후속 Repository 구현에서 연결한다. */
@Database(entities = [CardEntity::class, ExpenseEntity::class], version = 1, exportSchema = true)
@ConstructedBy(WoorisseumDatabaseConstructor::class)
internal abstract class WoorisseumDatabase : RoomDatabase() {
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
