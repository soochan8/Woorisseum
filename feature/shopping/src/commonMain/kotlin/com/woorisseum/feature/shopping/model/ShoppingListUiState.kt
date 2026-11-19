package com.woorisseum.feature.shopping.model

import com.woorisseum.core.domain.shopping.ShoppingTrip
import com.woorisseum.core.domain.shopping.ShoppingItem
import com.woorisseum.core.domain.shopping.ShoppingCategory

/** 화면에서만 사용하는 임시 목록이다. 저장·공유된 목록으로 표시하지 않는다. */
data class ShoppingListUiState(
    val items: List<ShoppingItem> = emptyList(),
    val trips: List<ShoppingTrip> = emptyList(),
    val finishItemIds: Set<String>? = null,
    val quickNameInput: String = "",
    val quickCategory: ShoppingCategory? = null,
    val categoryFilter: ShoppingCategoryFilter = ShoppingCategoryFilter.All,
    val quickNameError: ShoppingNameError? = null,
    val duplicateItemId: String? = null,
    val purchasedExpanded: Boolean = false,
    val editor: ShoppingItemEditorUiState? = null,
    val presence: ShoppingPresenceUiState = ShoppingPresenceUiState.NotConnected,
) {
    val totalPurchasedCount: Int get() = items.count { it.purchased }
    val itemsToFinish: List<ShoppingItem> get() = items.filter { it.purchased && it.id in finishItemIds.orEmpty() }
    // 화면 그룹을 위한 투영이다. 원본 목록을 따로 복제해 보관하지 않는다.
    val currentEditedItem: ShoppingItem? get() = editor?.original?.id?.let { id -> items.firstOrNull { it.id == id } }
    val pendingItems: List<ShoppingItem> get() = items.filter { !it.purchased && categoryFilter.matches(it) }
    val purchasedItems: List<ShoppingItem> get() = items.filter { it.purchased && categoryFilter.matches(it) }
}

data class ShoppingItemEditorUiState(
    val original: ShoppingItem,
    val nameInput: String = original.name,
    val quantityInput: String = original.quantity.orEmpty(),
    val memoInput: String = original.memo.orEmpty(),
    val nameError: ShoppingNameError? = null,
    val category: ShoppingCategory? = original.category,
) {
    val hasChanges: Boolean get() = nameInput.trim() != original.name ||
        quantityInput.normalizedShoppingText() != original.quantity.normalizedShoppingText() ||
        memoInput.normalizedShoppingText() != original.memo.normalizedShoppingText() || category != original.category
}

enum class ShoppingNameError { REQUIRED, DUPLICATE }

sealed interface ShoppingListUiIntent {
    data object RequestFinish : ShoppingListUiIntent
    data object CancelFinish : ShoppingListUiIntent
    data object ConfirmFinish : ShoppingListUiIntent
    data class QuickNameChanged(val value: String) : ShoppingListUiIntent
    data class QuickCategoryChanged(val category: ShoppingCategory?) : ShoppingListUiIntent
    data class CategoryFilterChanged(val filter: ShoppingCategoryFilter) : ShoppingListUiIntent
    data class EditCategory(val category: ShoppingCategory?) : ShoppingListUiIntent
    data object AddItem : ShoppingListUiIntent
    data object OpenDuplicateItem : ShoppingListUiIntent
    data class SetPurchased(val itemId: String, val purchased: Boolean) : ShoppingListUiIntent
    data object TogglePurchasedSection : ShoppingListUiIntent
    data class OpenEditor(val itemId: String) : ShoppingListUiIntent
    data class EditName(val value: String) : ShoppingListUiIntent
    data class EditQuantity(val value: String) : ShoppingListUiIntent
    data class EditMemo(val value: String) : ShoppingListUiIntent
    data object SubmitEdit : ShoppingListUiIntent
    data object CancelEdit : ShoppingListUiIntent
    data class DeleteItem(val itemId: String) : ShoppingListUiIntent
}

/** 선택 입력의 빈 값과 앞뒤 공백을 비교·적용 경계에서 동일하게 처리한다. */
internal fun String?.normalizedShoppingText(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** 전체·미분류·지정 분류를 구분하며 원본 목록은 유지한다. */
sealed interface ShoppingCategoryFilter {
    data object All : ShoppingCategoryFilter
    data object Uncategorized : ShoppingCategoryFilter
    data class Category(val value: ShoppingCategory) : ShoppingCategoryFilter

    fun matches(item: ShoppingItem): Boolean = when (this) {
        All -> true
        Uncategorized -> item.category == null
        is Category -> item.category == value
    }
}
