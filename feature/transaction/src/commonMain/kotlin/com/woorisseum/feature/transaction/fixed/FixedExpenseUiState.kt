package com.woorisseum.feature.transaction.fixed

import com.woorisseum.core.domain.transaction.fixed.FixedExpensePaymentStatus
import com.woorisseum.core.domain.transaction.fixed.FixedExpenseType
import com.woorisseum.feature.transaction.model.ExpenseAmountInputError
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/** summary=null은 조회 미연결이다. 실제로 조회된 빈 목록과 구분한다. */
data class FixedExpenseListUiState(val month: YearMonth, val summary: FixedExpenseSummaryUiModel? = null)
data class FixedExpenseSummaryUiModel(val totalAmountText: String, val paidAmountText: String?, val items: List<FixedExpenseItemUiModel>)
data class FixedExpenseItemUiModel(
    val id: String, val type: FixedExpenseType, val name: String?,
    val amountText: String, val dueDate: LocalDate, val paymentStatus: FixedExpensePaymentStatus,
)

/** 등록과 수정이 공유하는 계획 입력이다. 실제 거래의 날짜·결제수단과 섞지 않는다. */
data class FixedExpenseFormUiState(
    val startMonth: YearMonth,
    val type: FixedExpenseType? = null,
    val nameInput: String = "",
    val amountInput: String = "",
    val billingDayInput: String = "",
    val memoInput: String = "",
    val typeRequired: Boolean = false,
    val nameRequired: Boolean = false,
    val amountError: ExpenseAmountInputError? = null,
    val invalidBillingDay: Boolean = false,
)
data class FixedExpenseEditorUiState(
    val planId: String?, val form: FixedExpenseFormUiState?,
    val hasChanges: Boolean = false, val showStorageUnavailable: Boolean = false,
)

sealed interface FixedExpenseListUiIntent {
    data object PreviousMonth : FixedExpenseListUiIntent
    data object NextMonth : FixedExpenseListUiIntent
    data object AddPlan : FixedExpenseListUiIntent
    data class EditPlan(val id: String) : FixedExpenseListUiIntent
}
sealed interface FixedExpenseListUiEffect {
    data class OpenEditor(val month: YearMonth, val planId: String? = null) : FixedExpenseListUiEffect
}
sealed interface FixedExpenseEditorUiIntent {
    data class TypeSelected(val type: FixedExpenseType) : FixedExpenseEditorUiIntent
    data class NameChanged(val value: String) : FixedExpenseEditorUiIntent
    data class AmountChanged(val value: String) : FixedExpenseEditorUiIntent
    data class BillingDayChanged(val value: String) : FixedExpenseEditorUiIntent
    data class MemoChanged(val value: String) : FixedExpenseEditorUiIntent
    data object PreviousStartMonth : FixedExpenseEditorUiIntent
    data object NextStartMonth : FixedExpenseEditorUiIntent
    data object Submit : FixedExpenseEditorUiIntent
    data object Cancel : FixedExpenseEditorUiIntent
}
sealed interface FixedExpenseEditorUiEffect { data object NavigateBack : FixedExpenseEditorUiEffect }
