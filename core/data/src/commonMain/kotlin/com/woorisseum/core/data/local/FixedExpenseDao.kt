package com.woorisseum.core.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FixedExpenseDao {
    @Insert
    suspend fun insertPlan(plan: FixedExpenseEntity)
    @Insert
    suspend fun insertVersion(version: FixedExpenseVersionEntity)
    @Upsert
    suspend fun saveVersion(version: FixedExpenseVersionEntity)

    @Query("SELECT * FROM fixed_expense_plans WHERE id = :id")
    suspend fun findPlan(id: String): FixedExpenseEntity?

    @Query("SELECT * FROM fixed_expense_versions WHERE planId = :id AND fromMonth <= :month ORDER BY fromMonth DESC LIMIT 1")
    suspend fun findVersion(id: String, month: Long): FixedExpenseVersionEntity?

    @Query("UPDATE fixed_expense_plans SET revision = revision + 1 WHERE id = :id AND revision = :expectedRevision")
    suspend fun advanceRevision(id: String, expectedRevision: Long): Int

    @Query("UPDATE fixed_expense_plans SET stoppedFromMonth = :month, revision = revision + 1 WHERE id = :id AND revision = :expectedRevision")
    suspend fun stop(id: String, month: Long, expectedRevision: Long): Int

    @Query("""
        SELECT v.*, p.revision, p.stoppedFromMonth
        FROM fixed_expense_versions v JOIN fixed_expense_plans p ON p.id = v.planId
        WHERE v.fromMonth <= :month AND (p.stoppedFromMonth IS NULL OR :month < p.stoppedFromMonth)
        AND NOT EXISTS (
            SELECT 1 FROM fixed_expense_versions next
            WHERE next.planId = v.planId AND next.fromMonth <= :month AND next.fromMonth > v.fromMonth
        )
        ORDER BY v.billingDay, v.planId
    """)
    fun observePlans(month: Long): Flow<List<FixedExpenseSnapshot>>
}
