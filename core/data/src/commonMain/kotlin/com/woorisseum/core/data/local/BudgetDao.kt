package com.woorisseum.core.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface BudgetDao {
    @Query("SELECT amount FROM monthly_budgets WHERE year = :year AND month = :month")
    fun observeAmount(year: Int, month: Int): Flow<Long?>

    @Upsert
    suspend fun save(budget: BudgetEntity)
}
