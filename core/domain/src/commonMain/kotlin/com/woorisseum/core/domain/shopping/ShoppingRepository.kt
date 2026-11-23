package com.woorisseum.core.domain.shopping

import com.woorisseum.core.domain.common.AppResult
import kotlinx.coroutines.flow.Flow

/** 카테고리와 현재 물품을 같은 시점의 목록으로 전달한다. 공유·완료 기록은 별도 책임이다. */
data class ShoppingList(val categories: List<ShoppingCategoryDefinition>, val items: List<ShoppingItem>)
data class ShoppingItemInput(val name: String, val quantity: String? = null, val memo: String? = null,
    val category: ShoppingCategory? = null)

interface ShoppingRepository {
    /** 최초 한 번만 기본 분류를 저장한다. 모두 삭제한 뒤에도 다시 생성하지 않는다. */
    suspend fun initializeCategories(defaults: List<ShoppingCategoryDefinition>): AppResult<Unit>
    /** 조회 실패를 한 번 전달한 뒤 종료한다. 재시도 시 다시 구독한다. */
    fun observeList(): Flow<AppResult<ShoppingList>>
    suspend fun addItem(input: ShoppingItemInput): AppResult<Unit>
    /** 내용 revision만 비교하고 구매 상태는 유지한다. 중복 이름·동시 수정은 Conflict다. */
    suspend fun updateItem(id: String, input: ShoppingItemInput, expectedRevision: Long): AppResult<Unit>
    suspend fun setPurchased(id: String, purchased: Boolean): AppResult<Unit>
    /** 이미 없는 항목의 삭제도 성공이다. */
    suspend fun deleteItem(id: String): AppResult<Unit>
    suspend fun addCategory(name: String): AppResult<Unit>
    suspend fun renameCategory(category: ShoppingCategory, name: String, expectedRevision: Long): AppResult<Unit>
    /** 현재 물품만 미분류로 옮기며 삭제와 이동은 함께 확정된다. */
    suspend fun deleteCategory(category: ShoppingCategory, expectedRevision: Long): AppResult<Unit>
    /** 전체 배열 대신 이동 의도를 적용해 다른 변경을 덮어쓰지 않는다. */
    suspend fun moveCategory(category: ShoppingCategory, before: ShoppingCategory?): AppResult<Unit>
}
