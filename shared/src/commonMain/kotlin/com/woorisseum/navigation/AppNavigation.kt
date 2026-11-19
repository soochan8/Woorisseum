package com.woorisseum.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.woorisseum.feature.budget.screen.MonthlyBudgetRoute
import com.woorisseum.feature.shopping.screen.ShoppingHistoryRoute
import com.woorisseum.feature.shopping.screen.ShoppingListRoute
import com.woorisseum.feature.transaction.card.CardEditorRoute
import com.woorisseum.feature.transaction.card.CardListRoute
import com.woorisseum.feature.transaction.fixed.FixedExpenseEditorRoute
import com.woorisseum.feature.transaction.fixed.FixedExpenseListRoute
import com.woorisseum.feature.transaction.screen.*
import com.woorisseum.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Serializable private data object ExpenseListDestination
@Serializable private data object ShoppingListDestination
@Serializable private data object MonthlyBudgetDestination
@Serializable private data object ManagementDestination
@Serializable private data object ShoppingHistoryDestination
@Serializable private data object ExpenseEntryDestination
@Serializable private data class ExpenseDetailDestination(val expenseId: String)
@Serializable private data class ExpenseEditDestination(val expenseId: String)
@Serializable private data class FixedExpenseListDestination(val year: Int, val month: Int)
@Serializable private data class FixedExpenseEditorDestination(val year: Int, val month: Int, val planId: String? = null)
@Serializable private data object CardListDestination
@Serializable private data class CardEditorDestination(val cardId: String? = null)

private const val REQUESTED_EXPENSE_MONTH = "requestedExpenseMonth"

private enum class AppTab(val route: Any, val label: StringResource, val icon: DrawableResource) {
    Expenses(ExpenseListDestination, Res.string.nav_expenses, Res.drawable.nav_expenses),
    Budget(MonthlyBudgetDestination, Res.string.nav_budget, Res.drawable.nav_budget),
    Shopping(ShoppingListDestination, Res.string.nav_shopping, Res.drawable.nav_shopping),
    Management(ManagementDestination, Res.string.nav_management, Res.drawable.nav_management);

    fun matches(destination: NavDestination?): Boolean = when (this) {
        Expenses -> destination?.hasRoute<ExpenseListDestination>() == true
        Budget -> destination?.hasRoute<MonthlyBudgetDestination>() == true
        Shopping -> destination?.hasRoute<ShoppingListDestination>() == true
        Management -> destination?.hasRoute<ManagementDestination>() == true
    }
}

/** 선택된 탭을 다시 눌러도 초기화하지 않는다. 탭별 back stack과 Compose 상태를 저장·복원한다. */
private fun NavHostController.selectTab(tab: AppTab) {
    if (tab.matches(currentDestination)) return
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun currentMonth(): YearMonth = Clock.System.todayIn(TimeZone.currentSystemDefault()).let { YearMonth(it.year, it.month) }

/** Android·iOS 공통 앱 내비게이션과 탭 구성을 소유한다. */
@Composable
internal fun AppNavigation() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val selectedTab = AppTab.entries.firstOrNull { it.matches(currentEntry?.destination) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selectedTab != null) NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(selected = selectedTab == tab, onClick = { navController.selectTab(tab) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        ),
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) })
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = ExpenseListDestination,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            composable<ExpenseListDestination> { entry ->
                // 최상위 화면 상태는 탭 entry보다 오래 유지한다. 하위 입력 화면은 각 entry가 소유한다.
                val sessionEntry = remember(entry) { navController.getBackStackEntry(navController.graph.id) }
                val requestedMonth by entry.savedStateHandle.getStateFlow<String?>(REQUESTED_EXPENSE_MONTH, null).collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalViewModelStoreOwner provides sessionEntry) {
                    ExpenseListRoute(
                        onOpenExpense = { navController.navigate(ExpenseDetailDestination(it)) { launchSingleTop = true } },
                        onAddExpense = { navController.navigate(ExpenseEntryDestination) { launchSingleTop = true } },
                        requestedMonth = requestedMonth?.let(YearMonth::parse),
                        onMonthHandled = { entry.savedStateHandle[REQUESTED_EXPENSE_MONTH] = null },
                    )
                }
            }
            composable<MonthlyBudgetDestination> { entry ->
                val sessionEntry = remember(entry) { navController.getBackStackEntry(navController.graph.id) }
                val initialMonth = remember(entry) { currentMonth() }
                CompositionLocalProvider(LocalViewModelStoreOwner provides sessionEntry) {
                    MonthlyBudgetRoute(initialMonth = initialMonth, showBackButton = false,
                        onBack = { navController.selectTab(AppTab.Expenses) },
                        onViewExpenses = { month ->
                            navController.selectTab(AppTab.Expenses)
                            // 요청은 처리 완료까지 보관한다. 일반 탭 이동은 월·필터를 변경하지 않는다.
                            navController.getBackStackEntry<ExpenseListDestination>().savedStateHandle[REQUESTED_EXPENSE_MONTH] = month.toString()
                        })
                }
            }
            composable<ShoppingListDestination> { entry ->
                val sessionEntry = remember(entry) { navController.getBackStackEntry(navController.graph.id) }
                CompositionLocalProvider(LocalViewModelStoreOwner provides sessionEntry) {
                    ShoppingListRoute(showBackButton = false,
                        onBack = { navController.selectTab(AppTab.Expenses) },
                        onOpenHistory = { navController.navigate(ShoppingHistoryDestination) { launchSingleTop = true } })
                }
            }
            composable<ManagementDestination> {
                ManagementScreen(
                    onOpenCards = { navController.navigate(CardListDestination) { launchSingleTop = true } },
                    onOpenFixedExpenses = {
                        val month = currentMonth()
                        navController.navigate(FixedExpenseListDestination(month.year, month.month.number)) { launchSingleTop = true }
                    })
            }
            composable<ShoppingHistoryDestination> { entry ->
                val sessionEntry = remember(entry) { navController.getBackStackEntry(navController.graph.id) }
                ShoppingHistoryRoute(onBack = { navController.popBackStack() }, viewModelStoreOwner = sessionEntry)
            }
            composable<ExpenseDetailDestination> { entry ->
                ExpenseDetailRoute(expenseId = entry.toRoute<ExpenseDetailDestination>().expenseId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(ExpenseEditDestination(it)) { launchSingleTop = true } })
            }
            composable<ExpenseEditDestination> { entry ->
                ExpenseEditRoute(expenseId = entry.toRoute<ExpenseEditDestination>().expenseId,
                    onBack = { navController.popBackStack() },
                    onOpenCards = { navController.navigate(CardListDestination) { launchSingleTop = true } })
            }
            composable<ExpenseEntryDestination> {
                ExpenseEntryRoute(onBack = { navController.popBackStack() },
                    onOpenCards = { navController.navigate(CardListDestination) { launchSingleTop = true } })
            }
            composable<CardListDestination> {
                CardListRoute(onBack = { navController.popBackStack() },
                    onOpenEditor = { navController.navigate(CardEditorDestination(it)) { launchSingleTop = true } })
            }
            composable<CardEditorDestination> { entry ->
                CardEditorRoute(cardId = entry.toRoute<CardEditorDestination>().cardId, onBack = { navController.popBackStack() })
            }
            composable<FixedExpenseListDestination> { entry ->
                val route = entry.toRoute<FixedExpenseListDestination>()
                FixedExpenseListRoute(YearMonth(route.year, route.month), onBack = { navController.popBackStack() },
                    onOpenEditor = { month, id -> navController.navigate(FixedExpenseEditorDestination(month.year, month.month.number, id)) { launchSingleTop = true } })
            }
            composable<FixedExpenseEditorDestination> { entry ->
                val route = entry.toRoute<FixedExpenseEditorDestination>()
                FixedExpenseEditorRoute(YearMonth(route.year, route.month), onBack = { navController.popBackStack() }, planId = route.planId)
            }
        }
    }
}
