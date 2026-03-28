package com.qian.scrollsanity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.qian.scrollsanity.data.local.usagestats.UsageStatsRepository
import com.qian.scrollsanity.data.local.user.UserPreferencesLocalDataSource
import com.qian.scrollsanity.data.old.perferences.PreferencesManager
import com.qian.scrollsanity.data.old.sync.UsageSyncHelper
import com.qian.scrollsanity.data.remote.firestore.FirestoreRepository
import com.qian.scrollsanity.data.remote.user.UserPreferencesRemoteDataSource
import com.qian.scrollsanity.data.repo.AuthRepoImpl
import com.qian.scrollsanity.data.repo.DashboardMetricsRepoImpl
import com.qian.scrollsanity.data.repo.GoalRepoImpl
import com.qian.scrollsanity.data.repo.InterestRepoImpl
import com.qian.scrollsanity.data.repo.UserProfileRepoImpl
import com.qian.scrollsanity.data.repo.UserSettingsRepoImpl
import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.usecase.old.dashboard.GetLiveDashboardSummaryUseCase
import com.qian.scrollsanity.domain.usecase.old.intervention.EnabledTrackedProvider
import com.qian.scrollsanity.domain.usecase.old.intervention.IntensityProvider
import com.qian.scrollsanity.domain.usecase.user.AddGoalUseCase
import com.qian.scrollsanity.domain.usecase.user.AddInterestUseCase
import com.qian.scrollsanity.domain.usecase.user.DeleteGoalUseCase
import com.qian.scrollsanity.domain.usecase.user.DeleteInterestUseCase
import com.qian.scrollsanity.domain.usecase.user.ObserveGoalsUseCase
import com.qian.scrollsanity.domain.usecase.user.ObserveInterestsUseCase
import com.qian.scrollsanity.domain.usecase.user.ObserveUserPreferencesUseCase
import com.qian.scrollsanity.domain.usecase.user.ObserveUserProfileUseCase
import com.qian.scrollsanity.domain.usecase.user.SetGoalAchievedUseCase
import com.qian.scrollsanity.domain.usecase.user.SetInterestAchievedUseCase
import com.qian.scrollsanity.domain.usecase.user.SetTrackedAppEnabledUseCase
import com.qian.scrollsanity.domain.usecase.user.SyncUserPreferencesFromRemoteUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateDisplayNameUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateInterventionIntensityUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateNicknameUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateNotificationsEnabledUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateToneStyleUseCase
import com.qian.scrollsanity.service.UsageSyncService
import com.qian.scrollsanity.ui.dashboard.DashboardViewModel
import com.qian.scrollsanity.ui.dashboard.DashboardViewModelFactory
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
import com.qian.scrollsanity.ui.settings.SettingsViewModelFactory
import com.qian.scrollsanity.ui.theme.AreteTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefsManager = PreferencesManager(this)

        MainScope().launch {
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
    data object Home : Screen("home", "Today", Icons.Default.Home)
    data object Focus : Screen("focus", "Focus", Icons.Rounded.Timer)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
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

    val settingsViewModelFactory = remember(context) {
        createSettingsViewModelFactory(context)
    }

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = settingsViewModelFactory
    )

    val liveDashboardSummaryUseCase = remember(context) {
        createLiveDashboardSummaryUseCase(context)
    }

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
                            selected = currentDestination
                                ?.hierarchy
                                ?.any { it.route == screen.route } == true,
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

            composable("onboarding") {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate("main") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("goals") {
                GoalsScreen(onBack = { navController.popBackStack() })
            }

            composable("interests") {
                InterestsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            navigation(startDestination = "login", route = "auth") {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            UsageSyncService.start(context)

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

            navigation(startDestination = Screen.Home.route, route = "main") {
                composable(Screen.Home.route) {
                    HomeScreen()
                }

                composable(Screen.Focus.route) {
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = DashboardViewModelFactory(liveDashboardSummaryUseCase)
                    )
                    FocusScreen(viewModel = dashboardViewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onOpenGoals = { navController.navigate("goals") },
                        onOpenInterests = { navController.navigate("interests") },
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

private fun createSettingsViewModelFactory(
    context: Context
): SettingsViewModelFactory {
    val firestoreRepository = FirestoreRepository()

    val authRepo = AuthRepoImpl()
    val userProfileRepo = UserProfileRepoImpl(firestoreRepository)
    val goalRepo = GoalRepoImpl(firestoreRepository)
    val interestRepo = InterestRepoImpl(firestoreRepository)

    val userPreferencesLocalDataSource = UserPreferencesLocalDataSource(context)
    val userPreferencesRemoteDataSource = UserPreferencesRemoteDataSource(firestoreRepository)
    val userSettingsRepo = UserSettingsRepoImpl(
        localDataSource = userPreferencesLocalDataSource,
        remoteDataSource = userPreferencesRemoteDataSource
    )

    return SettingsViewModelFactory(
        authRepo = authRepo,
        observeUserProfileUseCase = ObserveUserProfileUseCase(userProfileRepo),
        updateNicknameUseCase = UpdateNicknameUseCase(userProfileRepo),
        updateDisplayNameUseCase = UpdateDisplayNameUseCase(userProfileRepo),
        observeUserPreferencesUseCase = ObserveUserPreferencesUseCase(userSettingsRepo),
        updateInterventionIntensityUseCase = UpdateInterventionIntensityUseCase(userSettingsRepo),
        updateToneStyleUseCase = UpdateToneStyleUseCase(userSettingsRepo),
        updateNotificationsEnabledUseCase = UpdateNotificationsEnabledUseCase(userSettingsRepo),
        setTrackedAppEnabledUseCase = SetTrackedAppEnabledUseCase(userSettingsRepo),
        syncUserPreferencesFromRemoteUseCase = SyncUserPreferencesFromRemoteUseCase(userSettingsRepo),
        observeGoalsUseCase = ObserveGoalsUseCase(goalRepo),
        addGoalUseCase = AddGoalUseCase(goalRepo),
        setGoalAchievedUseCase = SetGoalAchievedUseCase(goalRepo),
        deleteGoalUseCase = DeleteGoalUseCase(goalRepo),
        observeInterestsUseCase = ObserveInterestsUseCase(interestRepo),
        addInterestUseCase = AddInterestUseCase(interestRepo),
        setInterestAchievedUseCase = SetInterestAchievedUseCase(interestRepo),
        deleteInterestUseCase = DeleteInterestUseCase(interestRepo)
    )
}

private fun createLiveDashboardSummaryUseCase(
    context: Context
): GetLiveDashboardSummaryUseCase {
    val prefsManager = PreferencesManager(context)
    val usageRepoReal = UsageStatsRepository(context)
    val localUsageRepo: LocalUsageRepo = usageRepoReal
    val dashboardMetricsRepo = DashboardMetricsRepoImpl(context)

    val enabledProvider = object : EnabledTrackedProvider {
        override suspend fun getEnabledTrackedIds(): Set<TrackedAppId> {
            return prefsManager.enabledTrackedApps.first()
        }
    }

    val intensityProvider = object : IntensityProvider {
        override suspend fun getIntensity(): String {
            return prefsManager.interventionIntensity.first()
        }
    }

    return GetLiveDashboardSummaryUseCase(
        localUsageRepo = localUsageRepo,
        enabledTrackedProvider = enabledProvider,
        dashboardMetricsRepo = dashboardMetricsRepo,
        intensityProvider = intensityProvider
    )
}