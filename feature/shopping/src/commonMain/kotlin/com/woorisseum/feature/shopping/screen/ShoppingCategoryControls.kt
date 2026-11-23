package com.woorisseum.feature.shopping.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.component.WoorisseumSecondaryButton
import com.woorisseum.core.designsystem.component.WoorisseumFilterChip
import com.woorisseum.core.domain.shopping.ShoppingCategory
import com.woorisseum.core.domain.shopping.ShoppingCategoryDefinition
import com.woorisseum.feature.shopping.model.ShoppingCategoryFilter
import com.woorisseum.feature.shopping.model.ShoppingListUiIntent
import com.woorisseum.feature.shopping.resources.*
import org.jetbrains.compose.resources.stringResource

/** 빠른 추가와 수정 시트에서 같은 선택지를 사용한다. null로 분류를 해제할 수 있다. */
@Composable
internal fun ShoppingCategorySelector(value: ShoppingCategory?, categories: List<ShoppingCategoryDefinition> = emptyList(), onBeforeOpen: () -> Unit = {}, onSelect: (ShoppingCategory?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        WoorisseumSecondaryButton(onClick = { onBeforeOpen(); expanded = true }, enabled = categories.isNotEmpty()) {
            Text(stringResource(Res.string.shopping_category_selection, shoppingCategoryLabel(value, categories)))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(shoppingCategoryLabel(null)) },
                onClick = { expanded = false; onSelect(null) })
            categories.forEach { definition ->
                val category = definition.category
                DropdownMenuItem(text = { Text(shoppingCategoryLabel(category, categories)) },
                    onClick = { expanded = false; onSelect(category) })
            }
        }
    }
}

@Composable
internal fun ShoppingCategoryFilters(selected: ShoppingCategoryFilter, categories: List<ShoppingCategoryDefinition>, onSelect: (ShoppingCategoryFilter) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WoorisseumFilterChip(selected = selected == ShoppingCategoryFilter.All,
            onClick = { onSelect(ShoppingCategoryFilter.All) }, label = { Text(stringResource(Res.string.shopping_category_all)) })
        WoorisseumFilterChip(selected = selected == ShoppingCategoryFilter.Uncategorized,
            onClick = { onSelect(ShoppingCategoryFilter.Uncategorized) }, label = { Text(shoppingCategoryLabel(null)) })
        categories.forEach { definition ->
            val category = definition.category
            val filter = ShoppingCategoryFilter.Category(category)
            WoorisseumFilterChip(selected = selected == filter, onClick = { onSelect(filter) }, label = { Text(shoppingCategoryLabel(category, categories)) })
        }
    }
}

@Composable
internal fun shoppingCategoryLabel(category: ShoppingCategory?, categories: List<ShoppingCategoryDefinition> = emptyList()): String {
    categories.firstOrNull { it.category == category }?.let { return it.name }
    return defaultShoppingCategoryLabel(category)
}

/** 운영 기본값은 리소스로 한 번 전달하고 이후에는 ViewModel의 편집된 목록을 사용한다. */
@Composable
internal fun InitializeShoppingCategories(onIntent: (ShoppingListUiIntent) -> Unit) {
    val defaults = ShoppingCategory.defaults.map { ShoppingCategoryDefinition(it, defaultShoppingCategoryLabel(it)) }
    val reservedNames = setOf(stringResource(Res.string.shopping_category_all), stringResource(Res.string.shopping_category_none))
    LaunchedEffect(defaults, reservedNames) {
        onIntent(ShoppingListUiIntent.InitializeCategories(defaults, reservedNames))
    }
}

@Composable
private fun defaultShoppingCategoryLabel(category: ShoppingCategory?): String = stringResource(when (category) {
    ShoppingCategory.PRODUCE -> Res.string.shopping_category_produce
    ShoppingCategory.MEAT_AND_SEAFOOD -> Res.string.shopping_category_meat
    ShoppingCategory.DAIRY_AND_EGGS -> Res.string.shopping_category_dairy
    ShoppingCategory.PANTRY -> Res.string.shopping_category_pantry
    ShoppingCategory.HOUSEHOLD -> Res.string.shopping_category_household
    ShoppingCategory.OTHER -> Res.string.shopping_category_other
    else -> Res.string.shopping_category_none
})
