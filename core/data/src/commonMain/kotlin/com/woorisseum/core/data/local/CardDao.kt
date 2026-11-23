package com.woorisseum.core.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CardDao {
    @Query("SELECT * FROM cards ORDER BY registrationOrder ASC")
    fun observeCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun findById(id: String): CardEntity?

    // 기본 ABORT 정책: ID 충돌 시 기존 카드를 덮어쓰지 않는다.
    @Insert
    suspend fun insert(card: CardEntity)

    /** 내부 등록 순서는 바꾸지 않으며 삭제된 카드를 재생성하지 않는다. */
    @Query("UPDATE cards SET alias = :alias, issuer = :issuer WHERE id = :id")
    suspend fun update(id: String, alias: String, issuer: String): Int

    /** 지출 스냅샷은 건드리지 않는다. 없는 ID의 삭제는 멱등적이다. */
    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: String)
}
