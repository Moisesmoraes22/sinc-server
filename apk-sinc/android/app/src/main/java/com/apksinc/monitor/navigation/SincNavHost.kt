package com.apksinc.monitor.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apksinc.monitor.R
import com.apksinc.monitor.ui.about.AboutScreen
import com.apksinc.monitor.ui.apistatus.ApiStatusScreen
import com.apksinc.monitor.ui.dashboard.DashboardScreen
import com.apksinc.monitor.ui.details.ServerDetailsScreen
import com.apksinc.monitor.ui.history.HistoryScreen
import com.apksinc.monitor.ui.settings.SettingsScreen
import com.apksinc.monitor.ui.theme.ApkSincColors

private object Routes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val API_STATUS = "api_status"
    const val DETAILS = "details/{serverId}"
    fun details(serverId: String) = "details/$serverId"
}

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomTab(Routes.HISTORY, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
    BottomTab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun SincNavHost(startServerId: String? = null) {
    val navController = rememberNavController()
    val colors = ApkSincColors.colors

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar(
                containerColor = colors.elevated,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp)),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            ) {
                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        label = { Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.accent,
                            selectedTextColor = colors.accent,
                            indicatorColor = colors.accentSoft,
                            unselectedIconColor = colors.textMuted,
                            unselectedTextColor = colors.textMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
            // Troca de aba (irmãs, sem hierarquia): fade + leve escala, sem
            // direção lateral - efeito "shared axis" em vez de corte seco.
            enterTransition = { fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.97f) },
            exitTransition = { fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 1.02f) },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(onServerClick = { id -> navController.navigate(Routes.details(id)) })
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onAboutClick = { navController.navigate(Routes.ABOUT) },
                    onApiStatusClick = { navController.navigate(Routes.API_STATUS) },
                )
            }
            composable(
                Routes.ABOUT,
                // Tela empilhada (push): desliza da direita e some para a
                // esquerda, como uma navegação real, não um fade seco.
                enterTransition = { slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(220)) },
                exitTransition = { slideOutHorizontally(tween(160)) { -it / 4 } + fadeOut(tween(120)) },
                popEnterTransition = { slideInHorizontally(tween(220)) { -it / 4 } + fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(160)) { it / 3 } + fadeOut(tween(140)) },
            ) { AboutScreen(onBack = { navController.popBackStack() }) }
            composable(
                Routes.API_STATUS,
                enterTransition = { slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(220)) },
                exitTransition = { slideOutHorizontally(tween(160)) { -it / 4 } + fadeOut(tween(120)) },
                popEnterTransition = { slideInHorizontally(tween(220)) { -it / 4 } + fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(160)) { it / 3 } + fadeOut(tween(140)) },
            ) { ApiStatusScreen(onBack = { navController.popBackStack() }) }
            composable(
                Routes.DETAILS,
                enterTransition = { slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(220)) },
                exitTransition = { slideOutHorizontally(tween(160)) { -it / 4 } + fadeOut(tween(120)) },
                popEnterTransition = { slideInHorizontally(tween(220)) { -it / 4 } + fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(160)) { it / 3 } + fadeOut(tween(140)) },
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                ServerDetailsScreen(serverId = serverId, onBack = { navController.popBackStack() })
            }
        }

        androidx.compose.runtime.LaunchedEffect(startServerId) {
            if (startServerId != null) {
                navController.navigate(Routes.details(startServerId))
            }
        }
    }
}
