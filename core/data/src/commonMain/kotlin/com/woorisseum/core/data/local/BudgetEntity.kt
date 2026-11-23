package com.woorisseum.core.data.local

import androidx.room3.Entity

/** 연·월 복합 기본키로 같은 달의 예산 중복을 방지한다. */
@Entity(tableName = "monthly_budgets", primaryKeys = ["year", "month"])
internal data class BudgetEntity(val year: Int, val month: Int, val amount: Long)
