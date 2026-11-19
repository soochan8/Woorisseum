package com.woorisseum.feature.shopping.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.core.domain.shopping.ShoppingItem
import com.woorisseum.feature.shopping.model.*

// 예시 물품·사용자는 Preview 전용이다. 운영 DI에서는 주입하지 않는다.
private fun previewItems() = listOf(
    ShoppingItem("milk", "우유", "2개"),
    ShoppingItem("egg", "계란", "1판"),
    ShoppingItem("tofu", "두부", "1모", "찌개용"),
    ShoppingItem("apple", "사과", "3개", purchased = true),
    ShoppingItem("rice", "쌀", "2kg", purchased = true),
)
private fun previewPresence(): ShoppingPresenceUiState {
    val partner = ShoppingCollaboratorUiModel("preview-partner", "수찬")
    return ShoppingPresenceUiState.Connected(listOf(partner), mapOf("egg" to listOf(partner)), mapOf("tofu" to partner))
}

@Preview(name = "장보기 · 공동 편집 표시", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ShoppingCollaborativePreview() = WoorisseumTheme {
    ShoppingListScreen(ShoppingListUiState(items = previewItems(), presence = previewPresence()), {}, {})
}

@Preview(name = "장보기 · 임시 목록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ShoppingTemporaryPreview() = WoorisseumTheme {
    ShoppingListScreen(ShoppingListUiState(items = previewItems(), purchasedExpanded = true), {}, {})
}

@Preview(name = "장보기 · 빈 목록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ShoppingEmptyPreview() = WoorisseumTheme { ShoppingListScreen(ShoppingListUiState(), {}, {}) }

@Preview(name = "장보기 · 중복 이름", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ShoppingDuplicatePreview() = WoorisseumTheme {
    ShoppingListScreen(ShoppingListUiState(items = previewItems(), quickNameInput = "우유",
        quickNameError = ShoppingNameError.DUPLICATE, duplicateItemId = "milk"), {}, {})
}

@Preview(name = "장보기 · 물품 수정", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ShoppingEditorPreview() = WoorisseumTheme {
    ShoppingItemEditorSheet(ShoppingItemEditorUiState(previewItems().first()), {}, purchased = previewItems().first().purchased)
}

@Preview(name = "장보기 · 좁은 화면", widthDp = 320, heightDp = 640, locale = "ko")
@Composable
private fun ShoppingNarrowPreview() = WoorisseumTheme {
    ShoppingListScreen(ShoppingListUiState(items = listOf(ShoppingItem("long", "오래 두고 먹을 냉동 채소와 과일",
        "500g 묶음 2개", "가격과 원산지 확인하기")), presence = previewPresence()), {}, {})
}

@Preview(name = "장보기 · 다크", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ShoppingDarkPreview() = WoorisseumTheme(darkTheme = true) {
    ShoppingListScreen(ShoppingListUiState(items = previewItems(), presence = previewPresence()), {}, {})
}

@Preview(name = "장보기 · 마치기 확인", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ShoppingFinishPreview() = WoorisseumTheme {
    ShoppingFinishDialog(completedCount = 2, remainingCount = 3, onCancel = {}, onConfirm = {})
}

@Preview(name = "장보기 · 완료 기록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ShoppingHistoryPreview() = WoorisseumTheme {
    ShoppingHistoryScreen(listOf(com.woorisseum.core.domain.shopping.ShoppingTrip(
        "preview-trip", kotlin.time.Instant.parse("2026-11-19T02:00:00Z"), previewItems().filter { it.purchased })), {})
}
