package com.woorisseum.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.woorisseum.feature.budget.screen.MonthlyBudgetRoute
import com.woorisseum.feature.shopping.screen.ShoppingCategoryRoute
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Serializable private data object AppGraph
@Serializable private data object ExpenseListDestination
@Serializable private data object ShoppingListDestination
@Serializable private data object MonthlyBudgetDestination
@Serializable private data object ManagementDestination
@Serializable private data object ShoppingCategoryDestination
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

/** 최상위 탭은 교체한다. 이전 탭은 뒤로가기 스택 대신 저장 상태로 보관한다. */
private fun NavHostController.selectTab(tab: AppTab) {
    if (tab.matches(currentDestination)) return
    navigate(tab.route) {
        popUpTo<AppGraph> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun currentMonth(): YearMonth = Clock.System.todayIn(TimeZone.currentSystemDefault()).let { YearMonth(it.year, it.month) }

private fun isTabSwitch(from: NavDestination, to: NavDestination): Boolean =
    AppTab.entries.any { it.matches(from) } && AppTab.entries.any { it.matches(to) }

private const val SCREEN_TRANSITION_MILLIS = 220

/** Android·iOS 공통 앱 내비게이션과 탭 구성을 소유한다. */
@Composable
internal fun AppNavigation() {
    val navController = rememberNavController()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val scope = rememberCoroutineScope()
    var leaving by remember { mutableStateOf(false) }
    val navigateBack: () -> Unit = {
        val entry = navController.currentBackStackEntry
        // 연타·전환 중의 뒤로가기로 이전 화면까지 중복 pop 하지 않는다.
        if (!leaving && entry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
            leaving = true
            scope.launch {
                try {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    // 키보드의 높이 변화와 화면의 가로 이동을 분리한다.
                    // OS가 닫힘을 알리지 않아도 뒤로가기를 계속 막지 않는다.
                    withTimeoutOrNull(500) {
                        snapshotFlow { imeInsets.getBottom(density) == 0 }.first { it }
                    }
                    if (navController.currentBackStackEntry == entry) navController.popBackStack()
                } finally {
                    leaving = false
                }
            }
        }
    }
    NavHost(navController, startDestination = ExpenseListDestination, route = AppGraph::class,
        modifier = Modifier.fillMaxSize().clipToBounds(),
        // 탭 화면은 자체 Scaffold를 가지므로 하위 화면 전환 중에도 높이가 바뀌지 않는다.
        // 기존 화면은 고정하고, 불투명한 새 화면이 덮으며 진입한다.
        enterTransition = {
            if (isTabSwitch(initialState.destination, targetState.destination)) EnterTransition.None
            else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(SCREEN_TRANSITION_MILLIS))
        },
        exitTransition = {
            if (isTabSwitch(initialState.destination, targetState.destination)) ExitTransition.None
            else ExitTransition.KeepUntilTransitionsFinished
        },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = {
            if (isTabSwitch(initialState.destination, targetState.destination)) ExitTransition.None
            else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(SCREEN_TRANSITION_MILLIS))
        },
    ) {
        composable<ExpenseListDestination> { entry ->
            AppTabScaffold(AppTab.Expenses, navController::selectTab) { screenModifier ->
                // 최상위 화면 상태는 탭 entry보다 오래 유지한다. 하위 입력 화면은 각 entry가 소유한다.
                val sessionEntry = remember(entry) { navController.getBackStackEntry<AppGraph>() }
                val requestedMonth by entry.savedStateHandle.getStateFlow<String?>(REQUESTED_EXPENSE_MONTH, null).collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalViewModelStoreOwner provides sessionEntry) {
                    ExpenseListRoute(
                        modifier = screenModifier,
                        onOpenExpense = { navController.navigate(ExpenseDetailDestination(it)) { launchSingleTop = true } },
                        onAddExpense = { navController.navigate(ExpenseEntryDestination) { launchSingleTop = true } },
                        requestedMonth = requestedMonth?.let(YearMonth::parse),
                        onMonthHandled = { entry.savedStateHandle[REQUESTED_EXPENSE_MONTH] = null },
                    )
                }
            }
        }
        composable<MonthlyBudgetDestination> { entry ->
            AppTabScaffold(AppTab.Budget, navController::selectTab) { screenModifier ->
                val sessionEntry = remember(entry) { navController.getBackStackEntry<AppGraph>() }
                val initialMonth = remember(entry) { currentMonth() }
                CompositionLocalProvider(LocalViewModelStoreOwner provides sessionEntry) {
                    MonthlyBudgetRoute(initialMonth = initialMonth, modifier = screenModifier, showBackButton = false,
                        onBack = { navController.selectTab(AppTab.Expenses) },
                        onViewExpenses = { month ->
                            navController.selectTab(AppTab.Expenses)
                            // 요청은 처리 완료까지 보관한다. 일반 탭 이동은 월·필터를 변경하지 않는다.
                            navController.getBackStackEntry<ExpenseListDestination>().savedStateHandle[REQUESTED_EXPENSE_MONTH] = month.toString()
                        })
                }
            }
        }
        composable<ShoppingListDestination> { entry ->
            AppTabScaffold(AppTab.Shopping, navController::selectTab) { screenModifier ->
                val sessionEntry = remember(entry) { navController.getBackStackEntry<AppGraph>() }
                CompositionLocalProvider(LocalViewModelStoreOwner provides sessionEntry) {
                    ShoppingListRoute(modifier = screenModifier, showBackButton = false,
                        onBack = { navController.selectTab(AppTab.Expenses) },
                        onOpenHistory = { navController.navigate(ShoppingHistoryDestination) { launchSingleTop = true } })
                }
            }
        }
        composable<ManagementDestination> {
            AppTabScaffold(AppTab.Management, navController::selectTab) { screenModifier ->
                ManagementScreen(
                    modifier = screenModifier,
                    onOpenShoppingCategories = { navController.navigate(ShoppingCategoryDestination) { launchSingleTop = true } },
                    onOpenCards = { navController.navigate(CardListDestination) { launchSingleTop = true } },
                    onOpenFixedExpenses = {
                        val month = currentMonth()
                        navController.navigate(FixedExpenseListDestination(month.year, month.month.number)) { launchSingleTop = true }
                    })
            }
        }
        composable<ShoppingCategoryDestination> { entry ->
            val sessionEntry = remember(entry) { navController.getBackStackEntry<AppGraph>() }
            ShoppingCategoryRoute(onBack = navigateBack, viewModelStoreOwner = sessionEntry)
        }
        composable<ShoppingHistoryDestination> { entry ->
            val sessionEntry = remember(entry) { navController.getBackStackEntry<AppGraph>() }
            ShoppingHistoryRoute(onBack = navigateBack, viewModelStoreOwner = sessionEntry)
        }
        composable<ExpenseDetailDestination> { entry ->
            ExpenseDetailRoute(expenseId = entry.toRoute<ExpenseDetailDestination>().expenseId,
                onBack = navigateBack,
                onEdit = { navController.navigate(ExpenseEditDestination(it)) { launchSingleTop = true } })
        }
        composable<ExpenseEditDestination> { entry ->
            ExpenseEditRoute(expenseId = entry.toRoute<ExpenseEditDestination>().expenseId,
                onBack = navigateBack,
                onOpenCards = { navController.navigate(CardListDestination) { launchSingleTop = true } })
        }
        composable<ExpenseEntryDestination> {
            ExpenseEntryRoute(onBack = navigateBack,
                onOpenCards = { navController.navigate(CardListDestination) { launchSingleTop = true } })
        }
        composable<CardListDestination> {
            CardListRoute(onBack = navigateBack,
                onOpenEditor = { navController.navigate(CardEditorDestination(it)) { launchSingleTop = true } })
        }
        composable<CardEditorDestination> { entry ->
            CardEditorRoute(cardId = entry.toRoute<CardEditorDestination>().cardId, onBack = navigateBack)
        }
        composable<FixedExpenseListDestination> { entry ->
            val route = entry.toRoute<FixedExpenseListDestination>()
            FixedExpenseListRoute(YearMonth(route.year, route.month), onBack = navigateBack,
                onOpenEditor = { month, id -> navController.navigate(FixedExpenseEditorDestination(month.year, month.month.number, id)) { launchSingleTop = true } })
        }
        composable<FixedExpenseEditorDestination> { entry ->
            val route = entry.toRoute<FixedExpenseEditorDestination>()
            FixedExpenseEditorRoute(YearMonth(route.year, route.month), onBack = navigateBack, planId = route.planId)
        }
    }
}

/** 탭 바는 탭 화면의 일부다. 이동 중인 다른 화면의 크기에 영향을 주지 않는다. */
@Composable
private fun AppTabScaffold(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        content(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding))
    }
}
