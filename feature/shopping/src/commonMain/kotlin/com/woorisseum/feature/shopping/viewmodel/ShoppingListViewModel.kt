package com.woorisseum.feature.shopping.viewmodel

import androidx.lifecycle.ViewModel
import com.woorisseum.core.domain.shopping.*
import com.woorisseum.feature.shopping.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid
import kotlin.time.Clock
import kotlin.time.Instant

/** 화면 생명주기 동안의 임시 목록만 소유한다. 실제 저장·접속 감지·공유는 수행하지 않는다. */
class ShoppingListViewModel(
    initialItems: List<ShoppingItem> = emptyList(),
    private val createId: () -> String = { Uuid.random().toString() },
    private val now: () -> Instant = { Clock.System.now() },
) : ViewModel() {
    init {
        require(initialItems.map { it.id }.distinct().size == initialItems.size)
        require(initialItems.map { it.name }.distinct().size == initialItems.size)
    }
    private val _uiState = MutableStateFlow(ShoppingListUiState(items = initialItems.toList()))
    val uiState = _uiState.asStateFlow()

    fun onIntent(intent: ShoppingListUiIntent) {
        // update의 재시도 안에서 ID 생성 같은 부수효과를 수행하지 않는다.
        val newId = if (intent == ShoppingListUiIntent.AddItem || intent == ShoppingListUiIntent.ConfirmFinish || intent == ShoppingListUiIntent.SubmitCategory) createId() else null
        val completedAt = if (intent == ShoppingListUiIntent.ConfirmFinish) now() else null
        _uiState.update { state ->
            when (intent) {
                is ShoppingListUiIntent.InitializeCategories -> if (state.categoriesInitialized) state else {
                    require(intent.categories.map { it.category }.distinct().size == intent.categories.size)
                    state.copy(categories = intent.categories.toList(), categoriesInitialized = true,
                        reservedCategoryNames = intent.reservedNames.toSet())
                }
                is ShoppingListUiIntent.MoveCategory -> state.moveCategory(intent.category, intent.before)
                is ShoppingListUiIntent.OpenCategoryEditor -> {
                    val category = intent.category?.let { id -> state.categories.find { it.category == id } }
                    if (!state.categoriesInitialized || (intent.category != null && category == null)) state
                    else state.copy(categoryEditor = ShoppingCategoryEditorUiState(category), categoryToDelete = null)
                }
                is ShoppingListUiIntent.CategoryNameChanged -> state.copy(categoryEditor = state.categoryEditor?.copy(nameInput = intent.value, error = null))
                ShoppingListUiIntent.SubmitCategory -> state.saveCategory(checkNotNull(newId))
                ShoppingListUiIntent.CancelCategoryEdit -> state.copy(categoryEditor = null)
                is ShoppingListUiIntent.RequestDeleteCategory -> if (state.categories.none { it.category == intent.category }) state else
                    state.copy(categoryToDelete = intent.category, categoryEditor = null)
                ShoppingListUiIntent.CancelDeleteCategory -> state.copy(categoryToDelete = null)
                ShoppingListUiIntent.ConfirmDeleteCategory -> state.deleteCategory()
                ShoppingListUiIntent.CloseCategoryManagement -> state.copy(categoryEditor = null, categoryToDelete = null)
                ShoppingListUiIntent.RequestFinish -> if (state.totalPurchasedCount == 0 || state.editor != null) state else
                    state.copy(finishItemIds = state.items.filter { it.purchased }.map { it.id }.toSet())
                ShoppingListUiIntent.CancelFinish -> state.copy(finishItemIds = null)
                ShoppingListUiIntent.ConfirmFinish -> {
                    val ids = state.finishItemIds
                    val completion = ids?.let { completeShopping(state.items, it, checkNotNull(newId), checkNotNull(completedAt),
                        state.categories.associate { category -> category.category to category.name }) }
                    if (completion == null) state.copy(finishItemIds = null) else {
                        check(state.trips.none { it.id == completion.trip.id }) { "Duplicate shopping trip identifier" }
                        state.copy(items = completion.remainingItems, trips = listOf(completion.trip) + state.trips,
                            finishItemIds = null, purchasedExpanded = false,
                            categoryFilter = ShoppingCategoryFilter.All, editor = null,
                            quickNameError = null, duplicateItemId = null)
                    }
                }
                is ShoppingListUiIntent.QuickNameChanged -> state.copy(quickNameInput = intent.value,
                    quickNameError = null, duplicateItemId = null)
                is ShoppingListUiIntent.QuickCategoryChanged -> if (state.acceptsCategory(intent.category)) state.copy(quickCategory = intent.category) else state
                is ShoppingListUiIntent.CategoryFilterChanged -> if (intent.filter !is ShoppingCategoryFilter.Category || state.acceptsCategory(intent.filter.value)) state.copy(categoryFilter = intent.filter) else state
                is ShoppingListUiIntent.EditCategory -> if (state.acceptsCategory(intent.category)) state.copy(editor = state.editor?.copy(category = intent.category)) else state
                ShoppingListUiIntent.AddItem -> {
                    val name = state.quickNameInput.trim()
                    val duplicate = findDuplicateShoppingItem(state.items, name)
                    when {
                        name.isEmpty() -> state.copy(quickNameError = ShoppingNameError.REQUIRED, duplicateItemId = null)
                        duplicate != null -> state.copy(quickNameError = ShoppingNameError.DUPLICATE, duplicateItemId = duplicate.id)
                        else -> {
                            val id = checkNotNull(newId)
                            check(state.items.none { it.id == id }) { "Duplicate shopping item identifier" }
                            val item = ShoppingItem(id, name, category = state.quickCategory)
                            state.copy(items = state.items + item, quickNameInput = "",
                                categoryFilter = if (state.categoryFilter.matches(item)) state.categoryFilter else ShoppingCategoryFilter.All,
                                quickNameError = null, duplicateItemId = null)
                        }
                    }
                }
                ShoppingListUiIntent.OpenDuplicateItem -> state.duplicateItemId?.let { state.openEditor(it) } ?: state
                is ShoppingListUiIntent.OpenEditor -> state.openEditor(intent.itemId)
                is ShoppingListUiIntent.SetPurchased -> state.copy(items = state.items.map { item ->
                    if (item.id == intent.itemId) item.changed(ShoppingItemChange.SetPurchased(intent.purchased)) else item
                })
                ShoppingListUiIntent.TogglePurchasedSection -> state.copy(purchasedExpanded = !state.purchasedExpanded)
                is ShoppingListUiIntent.EditName -> state.copy(editor = state.editor?.copy(nameInput = intent.value, nameError = null))
                is ShoppingListUiIntent.EditQuantity -> state.copy(editor = state.editor?.copy(quantityInput = intent.value))
                is ShoppingListUiIntent.EditMemo -> state.copy(editor = state.editor?.copy(memoInput = intent.value))
                is ShoppingListUiIntent.DeleteItem -> state.deleteItem(intent.itemId)
                ShoppingListUiIntent.CancelEdit -> state.copy(editor = null)
                ShoppingListUiIntent.SubmitEdit -> state.submitEdit()
            }
        }
    }
}

private fun ShoppingListUiState.openEditor(id: String): ShoppingListUiState {
    val item = items.find { it.id == id } ?: return this
    return copy(editor = ShoppingItemEditorUiState(item), quickNameError = null, duplicateItemId = null,
        purchasedExpanded = purchasedExpanded || item.purchased,
        categoryFilter = if (categoryFilter.matches(item)) categoryFilter else ShoppingCategoryFilter.All)
}

private fun ShoppingListUiState.submitEdit(): ShoppingListUiState {
    val draft = editor ?: return this
    val name = draft.nameInput.trim()
    if (name.isEmpty()) return copy(editor = draft.copy(nameError = ShoppingNameError.REQUIRED))
    if (findDuplicateShoppingItem(items, name, draft.original.id) != null) {
        return copy(editor = draft.copy(nameError = ShoppingNameError.DUPLICATE))
    }
    // 수정한 필드만 최신 항목에 적용한다. 편집 중 바뀐 구매 상태 등을 원본으로 덮어쓰지 않는다.
    val changes = buildList {
        if (draft.category != draft.original.category) add(ShoppingItemChange.ChangeCategory(draft.category))
        if (name != draft.original.name) add(ShoppingItemChange.Rename(name))
        val quantity = draft.quantityInput.normalizedShoppingText()
        if (quantity != draft.original.quantity.normalizedShoppingText()) add(ShoppingItemChange.ChangeQuantity(quantity))
        val memo = draft.memoInput.normalizedShoppingText()
        if (memo != draft.original.memo.normalizedShoppingText()) add(ShoppingItemChange.ChangeMemo(memo))
    }
    return copy(items = items.map { item ->
        if (item.id == draft.original.id) changes.fold(item) { current, change -> current.changed(change) } else item
    }, editor = null, quickNameError = null, duplicateItemId = null)
}

private fun ShoppingListUiState.deleteItem(id: String): ShoppingListUiState {
    if (items.none { it.id == id }) return this
    return copy(items = items.filterNot { it.id == id },
        editor = if (editor?.original?.id == id) null else editor,
        quickNameError = if (duplicateItemId == id) null else quickNameError,
        duplicateItemId = if (duplicateItemId == id) null else duplicateItemId)
}
