package com.mikec.macautravellogger.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.mikec.macautravellogger.ui.history.HistoryScreen
import com.mikec.macautravellogger.ui.home.HomeScreen
import com.mikec.macautravellogger.ui.report.ReportScreen
import com.mikec.macautravellogger.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home     : Screen("home",     "Home",     Icons.Filled.Home)
    data object History  : Screen("history",  "History",  Icons.Filled.History)
    data object Reports  : Screen("reports",  "Reports",  Icons.Filled.Assessment)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val items = listOf(Home, History, Reports, Settings)
    }
}

@Composable
fun MacauTravelLoggerNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                Screen.items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route)     { HomeScreen() }
            composable(Screen.History.route)  { HistoryScreen() }
            composable(Screen.Reports.route)  { ReportScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
