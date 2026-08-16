package com.apksinc.monitor.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apksinc.monitor.R
import com.apksinc.monitor.ui.dashboard.DashboardScreen
import com.apksinc.monitor.ui.details.ServerDetailsScreen
import com.apksinc.monitor.ui.history.HistoryScreen
import com.apksinc.monitor.ui.settings.SettingsScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val DETAILS = "details/{serverId}"
    fun details(serverId: String) = "details/$serverId"
}

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard),
    BottomTab(Routes.HISTORY, R.string.nav_history, Icons.Filled.History),
    BottomTab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun SincNavHost(startServerId: String? = null) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
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
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(onServerClick = { id -> navController.navigate(Routes.details(id)) })
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.DETAILS) { backStackEntry ->
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
