package com.woorisseum.feature.shopping.viewmodel

import com.woorisseum.core.domain.shopping.ShoppingCategory
import com.woorisseum.feature.shopping.model.ShoppingListUiState

internal fun ShoppingListUiState.acceptsCategory(category: ShoppingCategory?): Boolean =
    category == null || categories.any { it.category == category }
