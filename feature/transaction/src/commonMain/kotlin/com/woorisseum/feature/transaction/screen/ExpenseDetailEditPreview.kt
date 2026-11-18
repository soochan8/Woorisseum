package com.woorisseum.feature.transaction.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.feature.transaction.mapper.toDetailUiModel
import com.woorisseum.feature.transaction.mapper.toFormUiState
import com.woorisseum.feature.transaction.model.*
import kotlinx.datetime.LocalDate

// 예시 내역은 Preview에서만 생성한다. 앱 DI·목록의 초기 데이터로 사용하지 않는다.
private fun previewExpense() = Expense(
    id = "preview-expense", amount = 28_500, date = LocalDate(2026, 11, 18),
    merchant = "동네 마트", category = ExpenseCategory.VARIABLE,
    paymentMethod = PaymentMethod.Cash, memo = "이번 주 저녁 장보기",
)

@Preview(name = "지출 상세", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseDetailPreview() = WoorisseumTheme {
    val expense = previewExpense()
    ExpenseDetailScreen(ExpenseDetailUiState(expense.id, expense.toDetailUiModel()), {}, {})
}

@Preview(name = "지출 상세 · 선택값 없음", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseDetailEmptyFieldsPreview() = WoorisseumTheme {
    val expense = previewExpense().copy(merchant = null, memo = null)
    ExpenseDetailScreen(ExpenseDetailUiState(expense.id, expense.toDetailUiModel()), {}, {})
}

@Preview(name = "지출 상세 · 조회 미연결", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseDetailUnavailablePreview() = WoorisseumTheme {
    ExpenseDetailScreen(ExpenseDetailUiState("preview-expense"), {}, {})
}

@Preview(name = "지출 수정", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseEditPreview() = WoorisseumTheme {
    val expense = previewExpense()
    ExpenseEditScreen(ExpenseEditUiState(expense.id, expense.toFormUiState()), {})
}

@Preview(name = "지출 수정 · 입력 오류", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseEditErrorPreview() = WoorisseumTheme {
    val expense = previewExpense()
    ExpenseEditScreen(ExpenseEditUiState(expense.id, expense.toFormUiState().copy(
        amountInput = "0", amountError = ExpenseAmountInputError.NOT_POSITIVE,
    ), hasChanges = true), {})
}

@Preview(name = "지출 수정 · 조회 미연결", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseEditUnavailablePreview() = WoorisseumTheme {
    ExpenseEditScreen(ExpenseEditUiState("preview-expense"), {})
}

@Preview(name = "지출 상세 · 다크", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseDetailDarkPreview() = WoorisseumTheme(darkTheme = true) {
    val expense = previewExpense()
    ExpenseDetailScreen(ExpenseDetailUiState(expense.id, expense.toDetailUiModel()), {}, {})
}

@Preview(name = "지출 등록 · 메모", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun ExpenseEntryMemoPreview() = WoorisseumTheme {
    ExpenseEntryScreen(ExpenseEntryUiState(previewExpense().toFormUiState()), {}, {})
}
