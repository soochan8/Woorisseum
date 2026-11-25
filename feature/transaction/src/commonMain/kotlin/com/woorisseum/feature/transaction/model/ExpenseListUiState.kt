package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/** 목록 화면이 그릴 값이다. 합계·필터·날짜별 그룹은 화면 바깥에서 준비한다. */
data class ExpenseListUiState(
    val month: YearMonth,
    // 사용자별 원본·프로필 연결 전에는 null이다. 임의로 기존 지출을 두 사람에게 배분하지 않는다.
    val comparison: ExpenseComparisonUiModel? = null,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchPeriod: ExpenseSearchPeriod = ExpenseSearchPeriod(),
    // 로딩·실패를 빈 조회 성공과 구분한다.
    val isExpenseSourceReady: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: ExpenseOperationError? = null,
    val totalAmountText: String = "0",
    val totalAmountOverflow: Boolean = false,
    val expenseCount: Int = 0,
    val periodText: String = "",
    val filters: ExpenseListFilters = ExpenseListFilters(),
    // 조회 연결 시 필터 적용 전 월별 원본 목록으로 결정한다.
    val hasExpensesInMonth: Boolean = false,
    val availablePaymentMethods: List<PaymentMethod> = listOf(PaymentMethod.Cash, PaymentMethod.BankTransfer),
    val cardLoadFailed: Boolean = false,
    val dayGroups: List<ExpenseDayUiModel> = emptyList(),
)

/** 날짜별 헤더와 해당 날짜의 지출 행을 묶은 표시용 모델이다. */
data class ExpenseDayUiModel(
    val date: LocalDate,
    val dateLabel: String,
    val totalAmountText: String,
    val expenses: List<ExpenseItemUiModel>,
    val totalAmountOverflow: Boolean = false,
)

/** 금액·결제수단 이름 등 화면에 맞게 변환한 지출 한 줄이다. */
data class ExpenseItemUiModel(
    val id: String,
    val merchant: String,
    val amountText: String,
    val category: ExpenseCategory,
    val paymentMethodName: String,
    val memo: String? = null,
    val paymentMethod: PaymentMethod? = null,
)
