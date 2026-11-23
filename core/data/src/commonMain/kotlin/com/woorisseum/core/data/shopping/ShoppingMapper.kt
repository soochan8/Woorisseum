package com.woorisseum.core.data.shopping

import com.woorisseum.core.data.local.ShoppingCategoryEntity
import com.woorisseum.core.data.local.ShoppingItemEntity
import com.woorisseum.core.domain.shopping.*

internal fun ShoppingCategoryEntity.toDomain() = ShoppingCategoryDefinition(ShoppingCategory(id), name, revision)
internal fun ShoppingItemEntity.toDomain() = ShoppingItem(id, name, quantity, memo, purchased,
    categoryId?.let(::ShoppingCategory), revision)
internal fun ShoppingItemInput.toEntity(id: String) = ShoppingItemEntity(id = id, name = name.trim(),
    quantity = quantity.clean(), memo = memo.clean(), purchased = false, categoryId = category?.id)
private fun String?.clean() = this?.trim()?.takeIf { it.isNotEmpty() }
