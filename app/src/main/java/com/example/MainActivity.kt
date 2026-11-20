package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.LocalAppLanguage
import com.example.data.tr
import com.example.tutorial.AlgorithmRepository
import com.example.ui.screens.CompetitionTimerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SolverTimerScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TutorialScreen
import com.example.ui.theme.CubieKidTheme
import com.example.viewmodel.AppContainer
import com.example.viewmodel.CompetitionTimerViewModel
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.OnboardingViewModel
import com.example.viewmodel.ScannerViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.SolverTimerViewModel
import com.example.viewmodel.StatsViewModel
import com.example.viewmodel.TutorialViewModel
import com.example.viewmodel.UserProfileLoadState

object CubieRoutes {
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val SOLVER = "solver"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val TUTORIAL = "tutorial"
    const val COMPETITION_TIMER = "competition_timer"
}

data class TopLevelDestination(
    val route: String,
    val viLabel: String,
    val enLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = AppContainer(applicationContext)
        setContent {
            CubieKidTheme {
                CubieKidApp(appContainer = appContainer)
            }
        }
    }
}

@Composable
fun CubieKidApp(appContainer: AppContainer) {
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(appContainer.userProfileRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            appContainer.settingsRepository,
            appContainer.userProfileRepository
        )
    )

    val appSettings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val profileLoadState by onboardingViewModel.profileLoadState.collectAsStateWithLifecycle()
    val onboardingState by onboardingViewModel.onboardingState.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalAppLanguage provides appSettings.language) {
        when (profileLoadState) {
            UserProfileLoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            UserProfileLoadState.NeedsOnboarding -> {
                OnboardingScreen(
                    state = onboardingState,
                    viewModel = onboardingViewModel,
                    currentLanguage = appSettings.language,
                    onSwitchLanguage = { settingsViewModel.setLanguage(it) }
                )
            }
            is UserProfileLoadState.Ready -> {
                MainCubieNavHost(
                    appContainer = appContainer,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
private fun MainCubieNavHost(
    appContainer: AppContainer,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination(
            route = CubieRoutes.HOME,
            viLabel = "Trang chủ",
            enLabel = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_home"
        ),
        TopLevelDestination(
            route = CubieRoutes.SCANNER,
            viLabel = "Quét",
            enLabel = "Scan",
            selectedIcon = Icons.Filled.Videocam,
            unselectedIcon = Icons.Outlined.Videocam,
            testTag = "nav_scan"
        ),
        TopLevelDestination(
            route = CubieRoutes.SOLVER,
            viLabel = "Giải",
            enLabel = "Solve",
            selectedIcon = Icons.Filled.Extension,
            unselectedIcon = Icons.Outlined.Extension,
            testTag = "nav_solve"
        ),
        TopLevelDestination(
            route = CubieRoutes.STATS,
            viLabel = "Tiến độ",
            enLabel = "Stats",
            selectedIcon = Icons.Filled.BarChart,
            unselectedIcon = Icons.Outlined.BarChart,
            testTag = "nav_stats"
        ),
        TopLevelDestination(
            route = CubieRoutes.SETTINGS,
            viLabel = "Cài đặt",
            enLabel = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            appContainer.trainingRepository,
            appContainer.userProfileRepository,
            appContainer.wcaTimerRepository,
            appContainer
        )
    )
    val scannerViewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModel.Factory(appContainer)
    )
    val solverTimerViewModel: SolverTimerViewModel = viewModel(
        factory = SolverTimerViewModel.Factory(
            appContainer.trainingRepository,
            appContainer.settingsRepository,
            appContainer
        )
    )
    val statsViewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(appContainer.trainingRepository)
    )
    val tutorialViewModel: TutorialViewModel = viewModel(
        factory = TutorialViewModel.Factory(
            algorithmRepository = AlgorithmRepository(appContainer.appContext),
            userProfileRepository = appContainer.userProfileRepository,
            settingsRepository = appContainer.settingsRepository
        )
    )
    val competitionTimerViewModel: CompetitionTimerViewModel = viewModel(
        factory = CompetitionTimerViewModel.Factory(
            wcaTimerRepository = appContainer.wcaTimerRepository,
            settingsRepository = appContainer.settingsRepository
        )
    )

    val appSettings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by settingsViewModel.userProfile.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isFullscreenRoute = currentRoute == CubieRoutes.COMPETITION_TIMER

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp && !isFullscreenRoute

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = if (isFullscreenRoute) WindowInsets(0) else WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail && !isFullscreenRoute) {
                    NavigationBar {
                        destinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = tr(dest.viLabel, dest.enLabel)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1, softWrap = false) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isFullscreenRoute) Modifier else Modifier.padding(innerPadding))
            ) {
                if (useRail) {
                    NavigationRail {
                        destinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = tr(dest.viLabel, dest.enLabel)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1, softWrap = false) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = CubieRoutes.HOME,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable(CubieRoutes.HOME) {
                        val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
                        HomeScreen(
                            state = homeState,
                            onOpenScanner = { navigateTopLevel(CubieRoutes.SCANNER) },
                            onStartPractice = {
                                homeViewModel.startPracticeChallenge()
                                navigateTopLevel(CubieRoutes.SOLVER)
                            },
                            onOpenTutorial = { navController.navigate(CubieRoutes.TUTORIAL) },
                            onOpenCompetitionTimer = { navController.navigate(CubieRoutes.COMPETITION_TIMER) },
                            onOpenStats = { navigateTopLevel(CubieRoutes.STATS) },
                            onChangeDailyTarget = { homeViewModel.updateDailyTarget(it) }
                        )
                    }
                    composable(CubieRoutes.SCANNER) {
                        val scanState by scannerViewModel.uiState.collectAsStateWithLifecycle()
                        ScannerScreen(
                            state = scanState,
                            soundEnabled = appSettings.soundEnabled,
                            viewModel = scannerViewModel,
                            onCubeScannedSuccess = { navigateTopLevel(CubieRoutes.SOLVER) },
                            onSwitchToPractice = {
                                homeViewModel.startPracticeChallenge()
                                navigateTopLevel(CubieRoutes.SOLVER)
                            }
                        )
                    }
                    composable(CubieRoutes.SOLVER) {
                        val solverState by solverTimerViewModel.uiState.collectAsStateWithLifecycle()
                        SolverTimerScreen(
                            state = solverState,
                            viewModel = solverTimerViewModel,
                            onOpenStats = { navigateTopLevel(CubieRoutes.STATS) }
                        )
                    }
                    composable(CubieRoutes.STATS) {
                        val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()
                        StatsScreen(
                            state = statsState,
                            onFilterSelected = { statsViewModel.setFilter(it) },
                            onDeleteSession = { statsViewModel.deleteSession(it) }
                        )
                    }
                    composable(CubieRoutes.SETTINGS) {
                        SettingsScreen(
                            settings = appSettings,
                            userProfile = userProfile,
                            onSaveUserProfile = { name, age, cur, target, onRes ->
                                settingsViewModel.updateUserProfile(name, age, cur, target, onRes)
                            },
                            onSelectPlaySpeedMs = { settingsViewModel.setPlayStepDelayMs(it) },
                            onSelectLanguage = { settingsViewModel.setLanguage(it) },
                            onToggleSound = { settingsViewModel.setSoundEnabled(it) }
                        )
                    }
                    composable(CubieRoutes.TUTORIAL) {
                        val tutorialState by tutorialViewModel.uiState.collectAsStateWithLifecycle()
                        TutorialScreen(
                            state = tutorialState,
                            viewModel = tutorialViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(CubieRoutes.COMPETITION_TIMER) {
                        val timerState by competitionTimerViewModel.uiState.collectAsStateWithLifecycle()
                        CompetitionTimerScreen(
                            state = timerState,
                            viewModel = competitionTimerViewModel,
                            onExit = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
