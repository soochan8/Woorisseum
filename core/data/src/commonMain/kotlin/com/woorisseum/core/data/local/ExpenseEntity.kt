package com.woorisseum.core.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** 날짜는 epoch day, 금액은 원화 정수. 카드 정보는 삭제와 독립적인 당시 스냅샷이다. */
@Entity(tableName = "expenses", indices = [Index(value = ["dateEpochDay", "id"])])
internal data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    val dateEpochDay: Long,
    val merchant: String?,
    val category: String,
    val paymentType: String,
    val cardId: String?,
    val cardName: String?,
    val cardIssuer: String?,
    val memo: String?,
    // 전체 수정 시 오래된 초안의 덮어쓰기를 감지할 로컬 버전이다.
    val revision: Long = 0,
)
