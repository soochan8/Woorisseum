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
import com.woorisseum.feature.shopping.model.ShoppingCategoryFilter
import com.woorisseum.feature.shopping.resources.*
import org.jetbrains.compose.resources.stringResource

/** 빠른 추가와 수정 시트에서 같은 선택지를 사용한다. null로 분류를 해제할 수 있다. */
@Composable
internal fun ShoppingCategorySelector(value: ShoppingCategory?, onSelect: (ShoppingCategory?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        WoorisseumSecondaryButton(onClick = { expanded = true }) {
            Text(stringResource(Res.string.shopping_category_selection, shoppingCategoryLabel(value)))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(shoppingCategoryLabel(null)) },
                onClick = { expanded = false; onSelect(null) })
            ShoppingCategory.entries.forEach { category ->
                DropdownMenuItem(text = { Text(shoppingCategoryLabel(category)) },
                    onClick = { expanded = false; onSelect(category) })
            }
        }
    }
}

@Composable
internal fun ShoppingCategoryFilters(selected: ShoppingCategoryFilter, onSelect: (ShoppingCategoryFilter) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WoorisseumFilterChip(selected = selected == ShoppingCategoryFilter.All,
            onClick = { onSelect(ShoppingCategoryFilter.All) }, label = { Text(stringResource(Res.string.shopping_category_all)) })
        WoorisseumFilterChip(selected = selected == ShoppingCategoryFilter.Uncategorized,
            onClick = { onSelect(ShoppingCategoryFilter.Uncategorized) }, label = { Text(shoppingCategoryLabel(null)) })
        ShoppingCategory.entries.forEach { category ->
            val filter = ShoppingCategoryFilter.Category(category)
            WoorisseumFilterChip(selected = selected == filter, onClick = { onSelect(filter) }, label = { Text(shoppingCategoryLabel(category)) })
        }
    }
}

@Composable
internal fun shoppingCategoryLabel(category: ShoppingCategory?): String = stringResource(when (category) {
    null -> Res.string.shopping_category_none
    ShoppingCategory.PRODUCE -> Res.string.shopping_category_produce
    ShoppingCategory.MEAT_AND_SEAFOOD -> Res.string.shopping_category_meat
    ShoppingCategory.DAIRY_AND_EGGS -> Res.string.shopping_category_dairy
    ShoppingCategory.PANTRY -> Res.string.shopping_category_pantry
    ShoppingCategory.HOUSEHOLD -> Res.string.shopping_category_household
    ShoppingCategory.OTHER -> Res.string.shopping_category_other
})
