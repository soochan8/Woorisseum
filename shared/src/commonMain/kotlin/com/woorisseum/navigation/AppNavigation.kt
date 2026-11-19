package com.woorisseum.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.woorisseum.feature.budget.screen.MonthlyBudgetRoute
import com.woorisseum.feature.transaction.screen.ExpenseDetailRoute
import com.woorisseum.feature.transaction.screen.ExpenseEditRoute
import com.woorisseum.feature.transaction.fixed.FixedExpenseListRoute
import com.woorisseum.feature.transaction.fixed.FixedExpenseEditorRoute
import com.woorisseum.feature.transaction.screen.ExpenseEntryRoute
import com.woorisseum.feature.transaction.screen.ExpenseListRoute
import kotlinx.serialization.Serializable
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

@Serializable
private data class ExpenseListDestination(val year: Int? = null, val month: Int? = null)

@Serializable
private data class ExpenseDetailDestination(val expenseId: String)

@Serializable
private data class ExpenseEditDestination(val expenseId: String)

@Serializable
private data class FixedExpenseListDestination(val year: Int, val month: Int)

@Serializable
private data class FixedExpenseEditorDestination(val year: Int, val month: Int, val planId: String? = null)

@Serializable
private data object ExpenseEntryDestination

@Serializable
private data class MonthlyBudgetDestination(val year: Int, val month: Int)

/** 앱의 이동 경로와 back stack을 소유하고 feature에는 이동 콜백만 전달한다. */
@Composable
internal fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ExpenseListDestination()) {
        composable<ExpenseListDestination> { entry ->
            val destination = entry.toRoute<ExpenseListDestination>()
            ExpenseListRoute(
                initialMonth = destination.year?.let { year -> destination.month?.let { YearMonth(year, it) } },
                onOpenExpense = { id ->
                    navController.navigate(ExpenseDetailDestination(id)) { launchSingleTop = true }
                },
                onAddExpense = {
                    navController.navigate(ExpenseEntryDestination) { launchSingleTop = true }
                },
                onOpenFixedExpenses = { month ->
                    navController.navigate(FixedExpenseListDestination(month.year, month.month.number)) { launchSingleTop = true }
                },
                onOpenBudget = { month ->
                    navController.navigate(MonthlyBudgetDestination(month.year, month.month.number)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<ExpenseDetailDestination> { entry ->
            val destination = entry.toRoute<ExpenseDetailDestination>()
            ExpenseDetailRoute(
                expenseId = destination.expenseId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(ExpenseEditDestination(id)) { launchSingleTop = true } },
            )
        }
        composable<ExpenseEditDestination> { entry ->
            val destination = entry.toRoute<ExpenseEditDestination>()
            ExpenseEditRoute(expenseId = destination.expenseId, onBack = { navController.popBackStack() })
        }
        composable<FixedExpenseListDestination> { entry ->
            val destination = entry.toRoute<FixedExpenseListDestination>()
            FixedExpenseListRoute(YearMonth(destination.year, destination.month),
                onBack = { navController.popBackStack() },
                onOpenEditor = { month, id ->
                    navController.navigate(FixedExpenseEditorDestination(month.year, month.month.number, id)) { launchSingleTop = true }
                })
        }
        composable<FixedExpenseEditorDestination> { entry ->
            val destination = entry.toRoute<FixedExpenseEditorDestination>()
            FixedExpenseEditorRoute(YearMonth(destination.year, destination.month),
                onBack = { navController.popBackStack() }, planId = destination.planId)
        }
        composable<ExpenseEntryDestination> {
            ExpenseEntryRoute(onBack = { navController.popBackStack() })
        }
        composable<MonthlyBudgetDestination> { entry ->
            val destination = entry.toRoute<MonthlyBudgetDestination>()
            MonthlyBudgetRoute(
                initialMonth = YearMonth(destination.year, destination.month),
                onBack = { navController.popBackStack() },
                onViewExpenses = { month ->
                    // 목록을 선택한 월의 전체 내역으로 교체해, 왕복할 때 이동 기록이 계속 쌓이지 않게 한다.
                    navController.navigate(ExpenseListDestination(month.year, month.month.number)) {
                        popUpTo<ExpenseListDestination> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
