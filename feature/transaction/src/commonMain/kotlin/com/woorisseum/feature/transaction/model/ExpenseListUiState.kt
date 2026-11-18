package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/** 목록 화면이 그릴 값이다. 합계·필터·날짜별 그룹은 화면 바깥에서 준비한다. */
data class ExpenseListUiState(
    val month: YearMonth,
    val totalAmountText: String = "0",
    val expenseCount: Int = 0,
    val periodText: String = "",
    val filters: ExpenseListFilters = ExpenseListFilters(),
    // 조회 연결 시 필터 적용 전 월별 원본 목록으로 결정한다.
    val hasExpensesInMonth: Boolean = false,
    val availablePaymentMethods: List<PaymentMethod> = listOf(PaymentMethod.Cash, PaymentMethod.BankTransfer),
    val dayGroups: List<ExpenseDayUiModel> = emptyList(),
)

/** 날짜별 헤더와 해당 날짜의 지출 행을 묶은 표시용 모델이다. */
data class ExpenseDayUiModel(
    val date: LocalDate,
    val dateLabel: String,
    val totalAmountText: String,
    val expenses: List<ExpenseItemUiModel>,
)

/** 금액·결제수단 이름 등 화면에 맞게 변환한 지출 한 줄이다. */
data class ExpenseItemUiModel(
    val id: String,
    val merchant: String,
    val amountText: String,
    val category: ExpenseCategory,
    val paymentMethodName: String,
)
