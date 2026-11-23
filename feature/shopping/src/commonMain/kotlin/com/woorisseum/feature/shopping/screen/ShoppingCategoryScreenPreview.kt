package com.woorisseum.feature.shopping.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.core.domain.shopping.ShoppingCategory
import com.woorisseum.core.domain.shopping.ShoppingCategoryDefinition
import com.woorisseum.core.domain.shopping.ShoppingItem
import com.woorisseum.feature.shopping.model.ShoppingListUiState

private fun previewCategoryState() = ShoppingListUiState(
    categoriesInitialized = true,
    categories = listOf(
        ShoppingCategoryDefinition(ShoppingCategory.PRODUCE, "채소·과일"),
        ShoppingCategoryDefinition(ShoppingCategory.DAIRY_AND_EGGS, "유제품·계란"),
        ShoppingCategoryDefinition(ShoppingCategory("preview-pets"), "반려동물 용품"),
    ),
    items = listOf(
        ShoppingItem("preview-milk", "우유", category = ShoppingCategory.DAIRY_AND_EGGS),
        ShoppingItem("preview-egg", "계란", purchased = true, category = ShoppingCategory.DAIRY_AND_EGGS),
    ),
)

@Preview(name = "카테고리 관리", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun CategoryManagementPreview() = WoorisseumTheme(darkTheme = false) {
    ShoppingCategoryScreen(previewCategoryState(), {}, {})
}

@Preview(name = "카테고리 삭제 · 사용 중 안내", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun CategoryDeletionPreview() = WoorisseumTheme(darkTheme = false) {
    ShoppingCategoryScreen(previewCategoryState().copy(categoryToDelete = ShoppingCategory.DAIRY_AND_EGGS), {}, {})
}

@Preview(name = "카테고리 관리 · 다크·큰 글씨", widthDp = 320, heightDp = 640, fontScale = 1.5f, locale = "ko")
@Composable
private fun CategoryManagementDarkPreview() = WoorisseumTheme(darkTheme = true) {
    ShoppingCategoryScreen(previewCategoryState(), {}, {})
}
