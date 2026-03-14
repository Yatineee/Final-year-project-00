package com.qian.scrollsanity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.qian.scrollsanity.data.FirestoreRepository
import com.qian.scrollsanity.data.PreferencesManager
import com.qian.scrollsanity.data.usagedata.UsageSyncHelper
import com.qian.scrollsanity.service.UsageSyncService
import com.qian.scrollsanity.ui.screens.FocusScreen
import com.qian.scrollsanity.ui.screens.GateScreen
import com.qian.scrollsanity.ui.screens.GoalsScreen
import com.qian.scrollsanity.ui.screens.HomeScreen
import com.qian.scrollsanity.ui.screens.InterestsScreen
import com.qian.scrollsanity.ui.screens.LoginScreen
import com.qian.scrollsanity.ui.screens.OnboardingScreen
import com.qian.scrollsanity.ui.screens.RegisterScreen
import com.qian.scrollsanity.ui.screens.SettingsScreen
import com.qian.scrollsanity.ui.settings.SettingsViewModel
import com.qian.scrollsanity.ui.theme.AreteTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start usage sync service if user is logged in
        val prefsManager = PreferencesManager(this)
        kotlinx.coroutines.MainScope().launch {
            prefsManager.isUserLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) {
                    UsageSyncService.start(this@MainActivity)
                }
            }
        }

        setContent {
            AreteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AreteApp()
                }
            }
        }
    }
}

// Navigation destinations
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Today", Icons.Default.Home)
    object Focus : Screen("focus", "Focus", Icons.Rounded.Timer)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Focus,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreteApp() {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // ✅ Create SettingsViewModel once (stable across recompositions)
    val settingsViewModel = remember {
        SettingsViewModel(
            firestoreRepo = FirestoreRepository(),
            preferencesManager = prefsManager
        )
    }

    // Determine if we should show bottom bar (only for main app screens)
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Focus.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "gate",
            modifier = Modifier.padding(innerPadding)
        ) {

            // 1) GATE (top-level)
            composable("gate") {
                GateScreen(
                    onGoLogin = {
                        navController.navigate("auth") {
                            popUpTo("gate") { inclusive = true }
                        }
                    },
                    onGoOnboarding = {
                        navController.navigate("onboarding") {
                            popUpTo("gate") { inclusive = true }
                        }
                    },
                    onGoMain = {
                        navController.navigate("main") {
                            popUpTo("gate") { inclusive = true }
                        }
                    }
                )
            }

            // 2) ONBOARDING (top-level)
            composable("onboarding") {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate("main") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            // Goals screen
            composable("goals") {
                GoalsScreen(onBack = { navController.popBackStack() })
            }

            composable("interests") {
                InterestsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 3) AUTH flow (nested)
            navigation(startDestination = "login", route = "auth") {

                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            UsageSyncService.start(context)

                            // ✅ go to gate, not main
                            navController.navigate("gate") {
                                popUpTo("auth") { inclusive = true }
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate("register")
                        }
                    )
                }

                composable("register") {
                    RegisterScreen(
                        onRegisterSuccess = {
                            UsageSyncService.start(context)

                            // ✅ go to gate, not main
                            navController.navigate("gate") {
                                popUpTo("auth") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
            }

            // 4) MAIN app flow (nested)
            navigation(startDestination = Screen.Home.route, route = "main") {

                composable(Screen.Home.route) { HomeScreen() }

                composable(Screen.Focus.route) { FocusScreen() }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onOpenGoals = { navController.navigate("goals") },
                        onOpenInterests = {navController.navigate("interests")},
                        onLogout = {
                            scope.launch {
                                UsageSyncService.stop(context)
                                UsageSyncHelper(context).reset()
                                prefsManager.clearUserSession()

                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}