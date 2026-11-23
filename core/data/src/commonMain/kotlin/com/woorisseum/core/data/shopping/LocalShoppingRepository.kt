package com.woorisseum.core.data.shopping

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteException
import com.woorisseum.core.data.local.*
import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.shopping.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

/** DB가 원본이다. 이름 중복 검사·참조 확인·쓰기를 같은 쓰기 트랜잭션에서 처리한다. */
internal class LocalShoppingRepository(private val database: WoorisseumDatabase) : ShoppingRepository {
    private val dao = database.shoppingDao()

    override suspend fun initializeCategories(defaults: List<ShoppingCategoryDefinition>): AppResult<Unit> = storage {
        database.withWriteTransaction {
            if (dao.initialized() == 0) {
                if (defaults.map { it.category }.distinct().size != defaults.size ||
                    defaults.map { it.name.lowercase() }.distinct().size != defaults.size) return@withWriteTransaction invalid()
                defaults.forEachIndexed { index, definition ->
                    dao.insertCategory(ShoppingCategoryEntity(definition.category.id, definition.name,
                        definition.name.lowercase(), index.toLong()))
                }
                dao.initialize(ShoppingSettingsEntity())
            }
            success()
        }
    }

    override fun observeList(): Flow<AppResult<ShoppingList>> = dao.observeChanges()
        .map<List<String>, AppResult<ShoppingList>> {
            database.withReadTransaction {
                AppResult.Success(ShoppingList(dao.categories().map { it.toDomain() }, dao.items().map { it.toDomain() }))
            }
        }.distinctUntilChanged().catch { error ->
            if (error is SQLiteException) emit(AppResult.Failure(AppError.Storage)) else throw error
        }

    override suspend fun addItem(input: ShoppingItemInput): AppResult<Unit> = storage {
        database.withWriteTransaction {
            if (!validInput(input)) return@withWriteTransaction invalid()
            if (dao.duplicateItem(input.name.trim(), "") > 0) return@withWriteTransaction conflict()
            dao.insertItem(input.toEntity(Uuid.random().toString()))
            success()
        }
    }

    override suspend fun updateItem(id: String, input: ShoppingItemInput, expectedRevision: Long): AppResult<Unit> = storage {
        database.withWriteTransaction {
            val current = dao.item(id) ?: return@withWriteTransaction missing()
            if (!validRevision(expectedRevision) || !validInput(input)) return@withWriteTransaction invalid()
            if (current.revision != expectedRevision || dao.duplicateItem(input.name.trim(), id) > 0) return@withWriteTransaction conflict()
            val value = input.toEntity(id)
            if (dao.updateItem(id, value.name, value.quantity, value.memo, value.categoryId, expectedRevision) != 1) {
                return@withWriteTransaction conflict()
            }
            success()
        }
    }

    override suspend fun setPurchased(id: String, purchased: Boolean): AppResult<Unit> = storage {
        if (dao.setPurchased(id, purchased) == 1) success() else missing()
    }
    override suspend fun deleteItem(id: String): AppResult<Unit> = storage { dao.deleteItem(id); success() }

    override suspend fun addCategory(name: String): AppResult<Unit> = storage {
        database.withWriteTransaction {
            val value = name.trim()
            if (value.isEmpty()) return@withWriteTransaction invalid()
            if (dao.duplicateCategory(value.lowercase(), "") > 0) return@withWriteTransaction conflict()
            val categories = dao.categories()
            categories.forEachIndexed { index, row -> dao.position(row.id, index.toLong()) }
            dao.insertCategory(ShoppingCategoryEntity(Uuid.random().toString(), value, value.lowercase(), categories.size.toLong()))
            success()
        }
    }

    override suspend fun renameCategory(category: ShoppingCategory, name: String, expectedRevision: Long): AppResult<Unit> = storage {
        database.withWriteTransaction {
            val current = dao.category(category.id) ?: return@withWriteTransaction missing()
            val value = name.trim()
            if (value.isEmpty() || !validRevision(expectedRevision)) return@withWriteTransaction invalid()
            if (current.revision != expectedRevision || dao.duplicateCategory(value.lowercase(), category.id) > 0) return@withWriteTransaction conflict()
            if (dao.renameCategory(category.id, value, value.lowercase(), expectedRevision) == 1) success() else conflict()
        }
    }

    override suspend fun deleteCategory(category: ShoppingCategory, expectedRevision: Long): AppResult<Unit> = storage {
        database.withWriteTransaction {
            val current = dao.category(category.id) ?: return@withWriteTransaction success()
            if (current.revision != expectedRevision) return@withWriteTransaction conflict()
            if (dao.items().any { it.categoryId == category.id && !validRevision(it.revision) }) return@withWriteTransaction invalid()
            dao.clearCategory(category.id)
            dao.deleteCategory(category.id)
            dao.categories().forEachIndexed { index, row -> dao.position(row.id, index.toLong()) }
            success()
        }
    }

    override suspend fun moveCategory(category: ShoppingCategory, before: ShoppingCategory?): AppResult<Unit> = storage {
        database.withWriteTransaction {
            val current = dao.categories()
            val moving = current.find { it.id == category.id } ?: return@withWriteTransaction missing()
            if (category == before) return@withWriteTransaction success()
            val reordered = current.filterNot { it.id == moving.id }.toMutableList()
            val target = if (before == null) reordered.size else reordered.indexOfFirst { it.id == before.id }
            if (target < 0) return@withWriteTransaction missing()
            reordered.add(target, moving)
            reordered.forEachIndexed { index, row -> if (row.position != index.toLong()) dao.position(row.id, index.toLong()) }
            success()
        }
    }

    private suspend fun validInput(input: ShoppingItemInput): Boolean = input.name.isNotBlank() &&
        (input.category?.let { dao.category(it.id) != null } != false)
}
private fun validRevision(value: Long) = value >= 0 && value < Long.MAX_VALUE
private fun success() = AppResult.Success(Unit)
private fun invalid() = AppResult.Failure(AppError.InvalidInput)
private fun conflict() = AppResult.Failure(AppError.Conflict)
private fun missing() = AppResult.Failure(AppError.NotFound)
private suspend fun <T> storage(block: suspend () -> AppResult<T>): AppResult<T> = try { block() }
catch (_: SQLiteException) { AppResult.Failure(AppError.Storage) }
