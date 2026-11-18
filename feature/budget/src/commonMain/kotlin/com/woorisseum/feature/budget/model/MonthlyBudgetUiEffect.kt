package com.woorisseum.feature.budget.model

import kotlinx.datetime.YearMonth

/** 표시할 월만 앱에 전달한다. feature가 다른 기능의 화면을 직접 생성하지 않는다. */
sealed interface MonthlyBudgetUiEffect {
    data class NavigateToExpenses(val month: YearMonth) : MonthlyBudgetUiEffect
}
