package com.woorisseum.feature.shopping.viewmodel

import com.woorisseum.core.domain.shopping.ShoppingCategory
import com.woorisseum.core.domain.shopping.ShoppingCategoryDefinition
import com.woorisseum.core.domain.shopping.validateShoppingCategoryName
import com.woorisseum.feature.shopping.model.ShoppingListUiState
import com.woorisseum.feature.shopping.model.ShoppingCategoryFilter

internal fun ShoppingListUiState.acceptsCategory(category: ShoppingCategory?): Boolean =
    category == null || categories.any { it.category == category }

/** 이름만 변경해도 물품·필터의 분류 ID는 유지된다. 완료 기록의 이름 스냅샷은 변경하지 않는다. */
internal fun ShoppingListUiState.saveCategory(newId: String): ShoppingListUiState {
    val draft = categoryEditor ?: return this
    val error = validateShoppingCategoryName(draft.nameInput, categories, draft.original?.category, reservedCategoryNames)
    if (error != null) return copy(categoryEditor = draft.copy(error = error))
    val id = draft.original?.category ?: ShoppingCategory(newId)
    if (draft.original == null) check(categories.none { it.category == id }) { "Duplicate shopping category identifier" }
    val category = ShoppingCategoryDefinition(id, draft.nameInput.trim())
    return copy(categories = if (draft.original == null) categories + category else
        categories.map { if (it.category == id) category else it }, categoryEditor = null)
}

/** 현재 목록만 미분류로 옮긴다. 완료 기록은 당시 ID·이름 스냅샷을 유지한다. */
internal fun ShoppingListUiState.deleteCategory(): ShoppingListUiState {
    val id = categoryToDelete ?: return this
    if (categories.none { it.category == id }) return copy(categoryToDelete = null)
    return copy(
        categories = categories.filterNot { it.category == id },
        items = items.map { if (it.category == id) it.copy(category = null) else it },
        quickCategory = quickCategory?.takeUnless { it == id },
        categoryFilter = if ((categoryFilter as? ShoppingCategoryFilter.Category)?.value == id)
            ShoppingCategoryFilter.All else categoryFilter,
        editor = editor?.let { draft ->
            draft.copy(original = if (draft.original.category == id) draft.original.copy(category = null) else draft.original,
                category = draft.category?.takeUnless { it == id })
        },
        categoryEditor = null,
        categoryToDelete = null,
    )
}
