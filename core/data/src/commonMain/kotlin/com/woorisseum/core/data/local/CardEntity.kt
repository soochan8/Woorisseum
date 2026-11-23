package com.woorisseum.core.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** 등록 순서는 DB 내부 키로, 외부 식별은 변경되지 않는 ID로 구분한다. */
@Entity(tableName = "cards", indices = [androidx.room3.Index(value = ["id"], unique = true)])
internal data class CardEntity(
    @PrimaryKey(autoGenerate = true) val registrationOrder: Long = 0,
    val id: String,
    val alias: String,
    val issuer: String,
)
