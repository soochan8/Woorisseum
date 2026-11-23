package com.woorisseum.core.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ShoppingDao {
    // 세 테이블의 변경을 감지한 뒤 Repository의 읽기 트랜잭션에서 일관된 목록을 읽는다.
    @Query("SELECT 'category:' || id || ':' || revision || ':' || position AS token FROM shopping_categories UNION ALL SELECT 'item:' || id || ':' || revision || ':' || purchased FROM shopping_items UNION ALL SELECT 'settings:' || id FROM shopping_settings ORDER BY token")
    fun observeChanges(): Flow<List<String>>
    @Query("SELECT * FROM shopping_categories ORDER BY position, id")
    suspend fun categories(): List<ShoppingCategoryEntity>
    @Query("SELECT * FROM shopping_items ORDER BY registrationOrder, id")
    suspend fun items(): List<ShoppingItemEntity>
    @Query("SELECT COUNT(*) FROM shopping_settings WHERE id = 1")
    suspend fun initialized(): Int
    @Insert suspend fun initialize(value: ShoppingSettingsEntity)
    @Insert suspend fun insertCategory(value: ShoppingCategoryEntity)
    @Insert suspend fun insertItem(value: ShoppingItemEntity)
    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun item(id: String): ShoppingItemEntity?
    @Query("SELECT * FROM shopping_categories WHERE id = :id")
    suspend fun category(id: String): ShoppingCategoryEntity?
    @Query("SELECT COUNT(*) FROM shopping_items WHERE name = :name AND id != :excluding")
    suspend fun duplicateItem(name: String, excluding: String): Int
    @Query("SELECT COUNT(*) FROM shopping_categories WHERE nameKey = :key AND id != :excluding")
    suspend fun duplicateCategory(key: String, excluding: String): Int
    @Query("UPDATE shopping_items SET name=:name, quantity=:quantity, memo=:memo, categoryId=:categoryId, revision=revision+1 WHERE id=:id AND revision=:revision")
    suspend fun updateItem(id: String, name: String, quantity: String?, memo: String?, categoryId: String?, revision: Long): Int
    @Query("UPDATE shopping_items SET purchased=:purchased WHERE id=:id")
    suspend fun setPurchased(id: String, purchased: Boolean): Int
    @Query("DELETE FROM shopping_items WHERE id=:id")
    suspend fun deleteItem(id: String)
    @Query("UPDATE shopping_categories SET name=:name, nameKey=:key, revision=revision+1 WHERE id=:id AND revision=:revision")
    suspend fun renameCategory(id: String, name: String, key: String, revision: Long): Int
    @Query("UPDATE shopping_items SET categoryId=NULL, revision=revision+1 WHERE categoryId=:id")
    suspend fun clearCategory(id: String)
    @Query("DELETE FROM shopping_categories WHERE id=:id")
    suspend fun deleteCategory(id: String)
    @Query("UPDATE shopping_categories SET position=:position WHERE id=:id")
    suspend fun position(id: String, position: Long)
}
