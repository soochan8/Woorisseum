package com.woorisseum.feature.budget.model

import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import kotlinx.datetime.YearMonth

/** 화면 표시값과 입력 상태다. DB 저장 상태나 Repository 역할을 대신하지 않는다. */
data class MonthlyBudgetUiState(
    val month: YearMonth,
    val summary: BudgetSummaryUiModel,
    val editor: BudgetEditorUiState? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

data class BudgetSummaryUiModel(
    val budgetAmountText: String?,
    val spentAmountText: String?,
    val headlineAmountText: String?,
    val headline: BudgetHeadline,
    val usage: BudgetUsage,
    val usagePercentText: String?,
    val isUsageAboveDisplayLimit: Boolean,
    val progress: Float,
    val categories: List<BudgetCategoryUiModel>,
    val totalOverflow: Boolean = false,
)

enum class BudgetHeadline { UNSET, WAITING, REMAINING, EXCEEDED, ZERO, OVERFLOW }
enum class BudgetUsage { UNSET, WAITING, ZERO_BUDGET, AVAILABLE, OVERFLOW }

data class BudgetCategoryUiModel(
    val category: ExpenseCategory,
    val amountText: String?,
    val sharePercentText: String?,
    val progress: Float,
    val amountOverflow: Boolean = false,
)

data class BudgetEditorUiState(
    val amountInput: String = "",
    val error: BudgetAmountInputError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)

enum class BudgetAmountInputError { REQUIRED, INVALID_INTEGER, OUT_OF_RANGE }
