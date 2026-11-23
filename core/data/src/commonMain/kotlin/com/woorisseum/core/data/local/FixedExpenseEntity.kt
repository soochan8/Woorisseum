package com.woorisseum.core.data.local

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/** 변경 이력과 독립적인 계획 ID·전체 revision·중단 적용 월을 보관한다. */
@Entity(tableName = "fixed_expense_plans")
internal data class FixedExpenseEntity(
    @PrimaryKey val id: String,
    val revision: Long,
    val stoppedFromMonth: Long?,
)

/** 연×12+(월-1)의 정수로 월을 비교한다. 이전 이력을 덮어쓰지 않는다. */
@Entity(tableName = "fixed_expense_versions", primaryKeys = ["planId", "fromMonth"],
    foreignKeys = [ForeignKey(entity = FixedExpenseEntity::class,
        parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.RESTRICT)])
internal data class FixedExpenseVersionEntity(
    val planId: String, val fromMonth: Long, val type: String, val name: String?,
    val amount: Long, val billingDay: Int, val memo: String?,
)

internal data class FixedExpenseSnapshot(
    val planId: String, val fromMonth: Long, val type: String, val name: String?,
    val amount: Long, val billingDay: Int, val memo: String?,
    val revision: Long, val stoppedFromMonth: Long?,
)
