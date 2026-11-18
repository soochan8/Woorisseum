package com.woorisseum.feature.budget.model

sealed interface MonthlyBudgetUiIntent {
    data object PreviousMonth : MonthlyBudgetUiIntent
    data object NextMonth : MonthlyBudgetUiIntent
    data object EditBudget : MonthlyBudgetUiIntent
    data class AmountChanged(val value: String) : MonthlyBudgetUiIntent
    data object CancelEditing : MonthlyBudgetUiIntent
    data object ApplyBudget : MonthlyBudgetUiIntent
    data object ViewExpenses : MonthlyBudgetUiIntent
}
