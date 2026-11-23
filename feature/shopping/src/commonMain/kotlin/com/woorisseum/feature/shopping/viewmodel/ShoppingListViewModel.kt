package com.woorisseum.feature.shopping.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.common.AppError
import com.woorisseum.core.domain.common.AppResult
import com.woorisseum.core.domain.shopping.*
import com.woorisseum.feature.shopping.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 목록의 원본은 Repository가 소유하고 ViewModel은 입력·관찰·저장 결과만 관리한다. */
class ShoppingListViewModel(private val repository: ShoppingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ShoppingListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    private var defaults: List<ShoppingCategoryDefinition>? = null
    private var observation: Job? = null
    private var pendingMove: ShoppingListUiIntent.MoveCategory? = null

    fun onIntent(intent: ShoppingListUiIntent) {
        when (intent) {
            is ShoppingListUiIntent.InitializeCategories -> {
                _uiState.update { it.copy(reservedCategoryNames = intent.reservedNames.toSet()) }
                if (defaults == null) { defaults = intent.categories.toList(); observe() }
                return
            }
            ShoppingListUiIntent.Retry -> { if (!_uiState.value.isSaving) observe(); return }
            ShoppingListUiIntent.AddItem -> { addItem(); return }
            ShoppingListUiIntent.SubmitEdit -> { editItem(); return }
            ShoppingListUiIntent.SubmitCategory -> { saveCategory(); return }
            ShoppingListUiIntent.ConfirmDeleteCategory -> {
                val state = _uiState.value
                val category = state.categories.find { it.category == state.categoryToDelete } ?: return
                write({ repository.deleteCategory(category.category, category.revision) }) {
                    it.copy(categoryToDelete = null, categoryEditor = null)
                }
                return
            }
            is ShoppingListUiIntent.MoveCategory -> {
                if (_uiState.value.categoryEditor == null && _uiState.value.categoryToDelete == null) {
                    if (_uiState.value.isSaving) pendingMove = intent
                    else write({ repository.moveCategory(intent.category, intent.before) }) { it }
                }
                return
            }
            is ShoppingListUiIntent.SetPurchased -> {
                write({ repository.setPurchased(intent.itemId, intent.purchased) }) { it }; return
            }
            is ShoppingListUiIntent.DeleteItem -> {
                write({ repository.deleteItem(intent.itemId) }) {
                    it.copy(editor = it.editor?.takeUnless { draft -> draft.original.id == intent.itemId })
                }
                return
            }
            else -> Unit
        }
        _uiState.update { state ->
            // 저장 중 초안 변경·닫기를 막아 성공 응답이 새로운 입력을 지우지 않게 한다.
            if (state.isSaving) return@update state
            when (intent) {
                ShoppingListUiIntent.DismissStorageError -> state.copy(operationError = null)
                ShoppingListUiIntent.ReloadEditor -> state.editor?.original?.id?.let { state.openEditor(it) } ?: state
                is ShoppingListUiIntent.OpenCategoryEditor -> {
                    val category = intent.category?.let { id -> state.categories.find { it.category == id } }
                    if (!state.canWrite || (intent.category != null && category == null)) state
                    else state.copy(categoryEditor = ShoppingCategoryEditorUiState(category), categoryToDelete = null, operationError = null)
                }
                is ShoppingListUiIntent.CategoryNameChanged -> state.copy(categoryEditor = state.categoryEditor?.copy(nameInput = intent.value, error = null))
                ShoppingListUiIntent.CancelCategoryEdit -> state.copy(categoryEditor = null, operationError = null)
                is ShoppingListUiIntent.RequestDeleteCategory -> if (!state.canWrite || state.categories.none { it.category == intent.category }) state else
                    state.copy(categoryToDelete = intent.category, categoryEditor = null, operationError = null)
                ShoppingListUiIntent.CancelDeleteCategory -> state.copy(categoryToDelete = null, operationError = null)
                ShoppingListUiIntent.CloseCategoryManagement -> state.copy(categoryEditor = null, categoryToDelete = null, operationError = null)
                // 완료 기록 저장을 연결하기 전에는 현재 물품을 제거하지 않는다.
                ShoppingListUiIntent.RequestFinish, ShoppingListUiIntent.ConfirmFinish -> state
                ShoppingListUiIntent.CancelFinish -> state.copy(finishItemIds = null)
                is ShoppingListUiIntent.QuickNameChanged -> state.copy(quickNameInput = intent.value, quickNameError = null, duplicateItemId = null)
                is ShoppingListUiIntent.QuickCategoryChanged -> if (state.acceptsCategory(intent.category)) state.copy(quickCategory = intent.category) else state
                is ShoppingListUiIntent.CategoryFilterChanged -> if (intent.filter !is ShoppingCategoryFilter.Category || state.acceptsCategory(intent.filter.value)) state.copy(categoryFilter = intent.filter) else state
                is ShoppingListUiIntent.EditCategory -> if (state.acceptsCategory(intent.category)) state.copy(editor = state.editor?.copy(category = intent.category)) else state
                ShoppingListUiIntent.OpenDuplicateItem -> state.duplicateItemId?.let { state.openEditor(it) } ?: state
                is ShoppingListUiIntent.OpenEditor -> state.openEditor(intent.itemId)
                ShoppingListUiIntent.TogglePurchasedSection -> state.copy(purchasedExpanded = !state.purchasedExpanded)
                is ShoppingListUiIntent.EditName -> state.copy(editor = state.editor?.copy(nameInput = intent.value, nameError = null))
                is ShoppingListUiIntent.EditQuantity -> state.copy(editor = state.editor?.copy(quantityInput = intent.value))
                is ShoppingListUiIntent.EditMemo -> state.copy(editor = state.editor?.copy(memoInput = intent.value))
                ShoppingListUiIntent.CancelEdit -> state.copy(editor = null, operationError = null)
                else -> state
            }
        }
    }

    private fun observe() {
        val initial = defaults ?: return
        observation?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        observation = viewModelScope.launch {
            when (val initialized = repository.initializeCategories(initial)) {
                is AppResult.Failure -> { _uiState.update { it.copy(isLoading = false, loadError = initialized.error.toUi()) }; return@launch }
                is AppResult.Success -> Unit
            }
            repository.observeList().collect { result ->
                _uiState.update { state -> when (result) {
                    is AppResult.Success -> state.withList(result.data)
                    is AppResult.Failure -> state.copy(isLoading = false, loadError = result.error.toUi())
                } }
            }
        }
    }

    private fun addItem() {
        val state = _uiState.value
        if (!state.canWrite) return
        val name = state.quickNameInput.trim()
        val duplicate = findDuplicateShoppingItem(state.items, name)
        if (name.isEmpty() || duplicate != null) {
            _uiState.update { it.copy(quickNameError = if (name.isEmpty()) ShoppingNameError.REQUIRED else ShoppingNameError.DUPLICATE,
                duplicateItemId = duplicate?.id) }; return
        }
        val input = ShoppingItemInput(name = name, category = state.quickCategory)
        write({ repository.addItem(input) }) {
            it.copy(quickNameInput = "", quickNameError = null, duplicateItemId = null,
                categoryFilter = when (val filter = it.categoryFilter) {
                    ShoppingCategoryFilter.All -> filter
                    ShoppingCategoryFilter.Uncategorized -> if (input.category == null) filter else ShoppingCategoryFilter.All
                    is ShoppingCategoryFilter.Category -> if (input.category == filter.value) filter else ShoppingCategoryFilter.All
                })
        }
    }

    private fun editItem() {
        val state = _uiState.value
        val draft = state.editor ?: return
        if (!state.canWrite) return
        val name = draft.nameInput.trim()
        if (name.isEmpty() || findDuplicateShoppingItem(state.items, name, draft.original.id) != null) {
            _uiState.update { it.copy(editor = draft.copy(nameError = if (name.isEmpty()) ShoppingNameError.REQUIRED else ShoppingNameError.DUPLICATE)) }; return
        }
        write({ repository.updateItem(draft.original.id, ShoppingItemInput(name, draft.quantityInput,
            draft.memoInput, draft.category), draft.original.revision) }) { it.copy(editor = null) }
    }

    private fun saveCategory() {
        val state = _uiState.value
        val draft = state.categoryEditor ?: return
        if (!state.canWrite) return
        val error = validateShoppingCategoryName(draft.nameInput, state.categories, draft.original?.category, state.reservedCategoryNames)
        if (error != null) { _uiState.update { it.copy(categoryEditor = draft.copy(error = error)) }; return }
        write({ draft.original?.let { repository.renameCategory(it.category, draft.nameInput, it.revision) }
            ?: repository.addCategory(draft.nameInput) }) { it.copy(categoryEditor = null) }
    }

    /** 성공 전 목록을 직접 변경하지 않는다. 실패 시 초안을 유지하고 재시도를 사용자에게 맡긴다. */
    private fun write(operation: suspend () -> AppResult<Unit>, onSuccess: (ShoppingListUiState) -> ShoppingListUiState) {
        if (!_uiState.value.canWrite) return
        _uiState.update { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            try {
                when (val result = operation()) {
                    is AppResult.Success -> _uiState.update { onSuccess(it).copy(isSaving = false) }
                    is AppResult.Failure -> _uiState.update { it.copy(isSaving = false, operationError = result.error.toUi()) }
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
                val move = pendingMove
                pendingMove = null
                if (move != null) onIntent(move)
            }
        }
    }
}

/** 조회 갱신은 입력 초안을 보존하고 사라진 참조·필터만 정리한다. */
private fun ShoppingListUiState.withList(list: ShoppingList): ShoppingListUiState {
    val ids = list.categories.map { it.category }.toSet()
    val itemIds = list.items.map { it.id }.toSet()
    return copy(items = list.items, categories = list.categories, categoriesInitialized = true, isLoading = false, loadError = null,
        quickCategory = quickCategory?.takeIf { it in ids },
        categoryFilter = if ((categoryFilter as? ShoppingCategoryFilter.Category)?.value?.let { it !in ids } == true) ShoppingCategoryFilter.All else categoryFilter,
        editor = editor?.takeIf { it.original.id in itemIds }?.let { draft ->
            draft.copy(category = draft.category?.takeIf { it in ids })
        },
        categoryEditor = categoryEditor?.takeIf { it.original == null || it.original.category in ids },
        categoryToDelete = categoryToDelete?.takeIf { it in ids },
        duplicateItemId = duplicateItemId?.takeIf { it in itemIds },
        quickNameError = if (duplicateItemId != null && duplicateItemId !in itemIds) null else quickNameError)
}
private fun ShoppingListUiState.openEditor(id: String): ShoppingListUiState {
    val item = items.find { it.id == id } ?: return this
    return copy(editor = ShoppingItemEditorUiState(item), quickNameError = null, duplicateItemId = null, operationError = null,
        purchasedExpanded = purchasedExpanded || item.purchased,
        categoryFilter = if (categoryFilter.matches(item)) categoryFilter else ShoppingCategoryFilter.All)
}
private fun AppError.toUi() = when (this) {
    AppError.Storage -> ShoppingStorageError.STORAGE
    AppError.NotFound -> ShoppingStorageError.NOT_FOUND
    AppError.Conflict -> ShoppingStorageError.CONFLICT
    AppError.InvalidInput -> ShoppingStorageError.INVALID_INPUT
}
