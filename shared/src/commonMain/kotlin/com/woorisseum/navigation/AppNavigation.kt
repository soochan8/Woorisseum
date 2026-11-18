package com.woorisseum.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woorisseum.feature.transaction.screen.ExpenseEntryRoute
import com.woorisseum.feature.transaction.screen.ExpenseListRoute
import kotlinx.serialization.Serializable

@Serializable
private data object ExpenseListDestination

@Serializable
private data object ExpenseEntryDestination

/** 앱의 이동 경로와 back stack을 소유하고 feature에는 이동 콜백만 전달한다. */
@Composable
internal fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ExpenseListDestination) {
        composable<ExpenseListDestination> {
            ExpenseListRoute(onAddExpense = {
                navController.navigate(ExpenseEntryDestination) { launchSingleTop = true }
            })
        }
        composable<ExpenseEntryDestination> {
            ExpenseEntryRoute(onBack = { navController.popBackStack() })
        }
    }
}
