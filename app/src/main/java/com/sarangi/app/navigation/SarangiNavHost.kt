package com.sarangi.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sarangi.app.ui.chat.ChatScreen
import com.sarangi.app.ui.chat.ChatViewModel
import com.sarangi.app.ui.dashboard.DashboardScreen
import com.sarangi.app.ui.dashboard.DashboardViewModel
import com.sarangi.app.ui.onboarding.OnboardingScreen
import com.sarangi.app.ui.onboarding.OnboardingViewModel
import com.sarangi.app.ui.session.SessionScreen
import com.sarangi.app.ui.session.SessionViewModel
import com.sarangi.app.ui.settings.SettingsScreen
import com.sarangi.app.ui.settings.SettingsViewModel
import com.sarangi.app.ui.dashboard.TeacherBriefingScreen

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Session, "Session", Icons.Default.MusicNote),
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Default.BarChart),
    BottomNavItem(Screen.Chat, "Chat", Icons.Default.Chat),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SarangiNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route != Screen.Onboarding.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.tertiary,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Onboarding.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                val viewModel: OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    viewModel = viewModel,
                    onOnboardingComplete = {
                        navController.navigate(Screen.Session.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Session.route) {
                val viewModel: SessionViewModel = hiltViewModel()
                SessionScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route)
                    },
                    onNavigateToChat = {
                        navController.navigate(Screen.Chat.route)
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTeacherBriefing = {
                        navController.navigate(Screen.TeacherBriefing.route)
                    }
                )
            }
            composable(Screen.Chat.route) {
                val viewModel: ChatViewModel = hiltViewModel()
                ChatScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = viewModel)
            }
            composable(Screen.TeacherBriefing.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                TeacherBriefingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
