package com.yuliang.app.ui

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.yuliang.app.YuliangApplication
import com.yuliang.app.ui.main.*
import com.yuliang.app.ui.components.YuliangBottomSheet
import com.yuliang.app.ui.components.YuliangIcon
import com.yuliang.app.ui.components.YuliangSnackbarHost
import com.yuliang.app.ui.plan.FixedExpenseScreen
import com.yuliang.app.ui.plan.MonthlyPlanScreen
import com.yuliang.app.ui.plan.PlanViewModel
import com.yuliang.app.ui.theme.MotionTokens
import com.yuliang.app.ui.theme.AppColors

private data class MainDestination(val route: String, val label: String, val icon: YuliangIcon)
private val mainDestinations = listOf(
    MainDestination("home", "首页", YuliangIcon.HOME),
    MainDestination("bills", "账单", YuliangIcon.BILLS),
    MainDestination("statistics", "统计", YuliangIcon.STATISTICS),
    MainDestination("profile", "我的", YuliangIcon.PROFILE),
)

@Composable
fun YuliangApp() {
    val nav = rememberNavController()
    val application = LocalContext.current.applicationContext as YuliangApplication
    val mainVm: MainViewModel = viewModel(factory = MainViewModel.Factory(application.container))
    val planVm: PlanViewModel = viewModel(factory = PlanViewModel.Factory(application.container))
    val state by mainVm.state.collectAsStateWithLifecycle()
    val lowRamDevice = remember(application) {
        (application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
    }
    val effectiveState = state.copy(reduceMotion = state.reduceMotion || !ValueAnimator.areAnimatorsEnabled() || lowRamDevice)
    val snackbar = remember { SnackbarHostState() }
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val isMain = currentRoute in mainDestinations.map(MainDestination::route)
    var recordOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mainVm) { mainVm.messages.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { YuliangSnackbarHost(snackbar) },
            bottomBar = {
                if (isMain) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    mainDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                nav.navigate(destination.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { com.yuliang.app.ui.components.YuliangIcon(destination.icon) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = AppColors.current.brandSoft,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
            floatingActionButton = {
                if (isMain && !recordOpen) ExtendedFloatingActionButton(
                    onClick = { recordOpen = true },
                    modifier = Modifier.testTag("open_record"),
                    text = { Text("记一笔") },
                    icon = { com.yuliang.app.ui.components.YuliangIcon(YuliangIcon.ADD) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            },
        ) { padding ->
            val transitionMs = if (effectiveState.reduceMotion) 0 else MotionTokens.Medium
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(transitionMs)) + scaleIn(tween(transitionMs), initialScale = .985f) },
                exitTransition = { fadeOut(tween(transitionMs / 2)) + scaleOut(tween(transitionMs / 2), targetScale = 1.01f) },
                popEnterTransition = { fadeIn(tween(transitionMs)) + scaleIn(tween(transitionMs), initialScale = 1.01f) },
                popExitTransition = { fadeOut(tween(transitionMs / 2)) + scaleOut(tween(transitionMs / 2), targetScale = .985f) },
            ) {
                composable("home") { HomeScreen(effectiveState, { nav.navigate("plan") }, { nav.navigate("bills") }) { nav.navigate("transaction/$it") } }
                composable("bills") { BillsScreen(effectiveState) { nav.navigate("transaction/$it") } }
                composable("statistics") { StatisticsScreen(effectiveState) }
                composable("profile") { ProfileScreen(effectiveState, { nav.navigate("plan") }, { nav.navigate("fixed") }, { nav.navigate("categories") }, { nav.navigate("data") }, { nav.navigate("about") }, mainVm::setReduceMotion) }
                composable("plan") { MonthlyPlanScreen(planVm) { nav.popBackStack() } }
                composable("fixed") { FixedExpenseScreen(planVm) { nav.popBackStack() } }
                composable("categories") { CategoryManagementScreen(effectiveState, mainVm) { nav.popBackStack() } }
                composable("data") { DataManagementScreen(effectiveState, mainVm) { nav.popBackStack() } }
                composable("about") { AboutScreen { nav.popBackStack() } }
                composable("transaction/{id}") { entry ->
                    TransactionDetailScreen(effectiveState, entry.arguments?.getString("id")?.toLongOrNull() ?: -1, { nav.popBackStack() }, mainVm)
                }
            }
        }

        YuliangBottomSheet(visible = recordOpen, onDismiss = { recordOpen = false }, reduceMotion = effectiveState.reduceMotion) {
            QuickRecordPanel(effectiveState, mainVm) { recordOpen = false }
        }
    }
}
