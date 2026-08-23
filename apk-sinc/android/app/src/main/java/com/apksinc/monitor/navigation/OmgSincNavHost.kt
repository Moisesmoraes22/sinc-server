package com.apksinc.monitor.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.habits.HabitsScreen
import com.apksinc.monitor.ui.habits.HabitsViewModel
import com.apksinc.monitor.ui.health.HealthScreen
import com.apksinc.monitor.ui.health.HealthViewModel
import com.apksinc.monitor.ui.home.HomeScreen
import com.apksinc.monitor.ui.home.HomeViewModel
import com.apksinc.monitor.ui.profile.ProfileScreen
import com.apksinc.monitor.ui.profile.ProfileViewModel
import com.apksinc.monitor.ui.theme.OmgSincTheme
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel

private object Routes {
    const val HOME = "home"
    const val HABITS = "habits"
    const val HEALTH = "health"
    const val PROFILE = "profile"
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Inicio", Icons.Filled.Home),
    BottomTab(Routes.HABITS, "Habitos", Icons.Filled.Checklist),
    BottomTab(Routes.HEALTH, "Saude", Icons.Filled.Favorite),
    BottomTab(Routes.PROFILE, "Perfil", Icons.Filled.Person),
)

/**
 * Navegacao do OMG SINC: 4 destinos principais, simetria com a resposta a
 * "onde estou / o que posso fazer" - Inicio resume, Habitos e onde se age,
 * Saude aprofunda metricas, Perfil concentra conta e preferencias.
 * "Bem-estar" e "Rotina" (sugestao original) nao viraram abas separadas de
 * proposito: seriam o mesmo dado (habitos) filtrado de outra forma - melhor
 * resolvido por categoria dentro de Habitos do que por uma tela a mais.
 */
@Composable
fun OmgSincNavHost() {
    val navController = rememberNavController()
    val factory = ViewModelFactoryProvider.factory()

    Scaffold(
        containerColor = OmgSincTheme.colors.background,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar(containerColor = OmgSincTheme.colors.elevated) {
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
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OmgSincTheme.colors.accent,
                            selectedTextColor = OmgSincTheme.colors.accent,
                            indicatorColor = OmgSincTheme.colors.accentSoft,
                            unselectedIconColor = OmgSincTheme.colors.textMuted,
                            unselectedTextColor = OmgSincTheme.colors.textMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(composeViewModel<HomeViewModel>(factory = factory))
            }
            composable(Routes.HABITS) {
                HabitsScreen(composeViewModel<HabitsViewModel>(factory = factory))
            }
            composable(Routes.HEALTH) {
                HealthScreen(composeViewModel<HealthViewModel>(factory = factory))
            }
            composable(Routes.PROFILE) {
                ProfileScreen(composeViewModel<ProfileViewModel>(factory = factory))
            }
        }
    }
}
