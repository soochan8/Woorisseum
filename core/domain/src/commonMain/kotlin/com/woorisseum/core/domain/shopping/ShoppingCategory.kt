package com.woorisseum.core.domain.shopping

import kotlin.jvm.JvmInline

/** 이름 변경과 무관한 분류 ID다. 미분류는 여전히 null로 표현한다. */
@JvmInline
value class ShoppingCategory(val id: String) {
    init { require(id.isNotBlank()) }

    companion object {
        val PRODUCE = ShoppingCategory("produce")
        val MEAT_AND_SEAFOOD = ShoppingCategory("meat-and-seafood")
        val DAIRY_AND_EGGS = ShoppingCategory("dairy-and-eggs")
        val PANTRY = ShoppingCategory("pantry")
        val HOUSEHOLD = ShoppingCategory("household")
        val OTHER = ShoppingCategory("other")
        val defaults = listOf(PRODUCE, MEAT_AND_SEAFOOD, DAIRY_AND_EGGS, PANTRY, HOUSEHOLD, OTHER)
    }
}

/** 편집 가능한 이름이다. 기본 분류의 번역은 feature에서 주입한다. */
data class ShoppingCategoryDefinition(val category: ShoppingCategory, val name: String, val revision: Long = 0) {
    init { require(name.isNotBlank() && name == name.trim()) }
}

enum class ShoppingCategoryNameError { REQUIRED, DUPLICATE, RESERVED }

/** 분류 이름은 앞뒤 공백을 제거해 비교한다. UI 제어용 이름도 일반 분류로 등록하지 않는다. */
fun validateShoppingCategoryName(
    name: String,
    categories: List<ShoppingCategoryDefinition>,
    excluding: ShoppingCategory? = null,
    reservedNames: Set<String> = emptySet(),
): ShoppingCategoryNameError? {
    val normalized = name.trim()
    return when {
        normalized.isEmpty() -> ShoppingCategoryNameError.REQUIRED
        reservedNames.any { it.equals(normalized, ignoreCase = true) } -> ShoppingCategoryNameError.RESERVED
        categories.any { it.category != excluding && it.name.lowercase() == normalized.lowercase() } -> ShoppingCategoryNameError.DUPLICATE
        else -> null
    }
}
