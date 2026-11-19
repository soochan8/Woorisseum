package com.woorisseum.feature.transaction.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.core.domain.transaction.card.RegisteredCard

private val previewCards = listOf(
    RegisteredCard("preview-1", "생활비 카드", "현대카드"),
    RegisteredCard("preview-2", "교통 카드", "신한카드"),
)

@Preview(name = "카드 · 빈 목록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun CardEmptyPreview() = WoorisseumTheme { CardListScreen(CardListUiState(), {}, {}) }

@Preview(name = "카드 · 목록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun CardListPreview() = WoorisseumTheme { CardListScreen(CardListUiState(previewCards), {}, {}) }

@Preview(name = "카드 · 등록", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun CardRegisterPreview() = WoorisseumTheme { CardEditorScreen(CardEditorUiState(), {}, {}) }

@Preview(name = "카드 · 수정", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun CardEditPreview() = WoorisseumTheme {
    CardEditorScreen(CardEditorUiState("preview-1", "생활비 카드", "현대카드"), {}, {})
}

@Preview(name = "카드 · 필수값 오류", widthDp = 320, heightDp = 640, locale = "ko")
@Composable
private fun CardValidationPreview() = WoorisseumTheme {
    CardEditorScreen(CardEditorUiState(aliasRequired = true, issuerRequired = true), {}, {})
}

@Preview(name = "카드 · 다크", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun CardDarkPreview() = WoorisseumTheme(darkTheme = true) {
    CardListScreen(CardListUiState(previewCards), {}, {})
}
