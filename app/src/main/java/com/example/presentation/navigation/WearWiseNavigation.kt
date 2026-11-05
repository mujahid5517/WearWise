package com.example.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.di.AppContainer
import com.example.domain.repository.AuthenticatedUser
import com.example.presentation.authentication.FirebaseAccountScreen
import com.example.presentation.authentication.AuthViewModel
import com.example.presentation.authentication.WelcomeScreen
import com.example.presentation.common.OfflineStatusBar
import com.example.presentation.common.LocalWearWiseProfile
import com.example.presentation.common.WearWiseBottomBar
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.planner.PlannerScreen
import com.example.presentation.planner.PlannerViewModel
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.profile.ProfileViewModel
import com.example.presentation.shopping.ShoppingScreen
import com.example.presentation.shopping.ShoppingViewModel
import com.example.presentation.tryon.TryOnScreen
import com.example.presentation.tryon.TryOnViewModel
import com.example.presentation.wardrobe.WardrobeScreen
import com.example.presentation.wardrobe.WardrobeViewModel
import com.example.presentation.coloranalysis.ColorAnalysisScreen
import com.example.presentation.coloranalysis.ColorAnalysisViewModel
import com.example.presentation.trips.CreateTripScreen
import com.example.presentation.trips.CreateTripViewModel
import com.example.presentation.trips.TripDetailScreen
import com.example.presentation.trips.TripDetailViewModel
import com.example.presentation.trips.TripsScreen
import com.example.presentation.trips.TripsViewModel
import com.example.ui.theme.DarkNavyBg

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object CreateAccount : Screen("create_account")
    data object SignIn : Screen("sign_in")
    data object Onboarding : Screen("onboarding")
    data object Outfits : Screen("outfits")
    data object Closet : Screen("closet")
    data object Avatar : Screen("avatar")
    data object Planner : Screen("planner")
    data object Profile : Screen("profile")
    data object Shopping : Screen("shopping")
    data object ColorAnalysis : Screen("color_analysis")
    data object Trips : Screen("trips")
    data object CreateTrip : Screen("create_trip")
    data object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: Long) = "trip_detail/$tripId"
    }
}

@Composable
fun WearWiseApp(container: AppContainer) {
    val authenticationState by container.authentication.state.collectAsState()
    if (!authenticationState.isReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
        }
        return
    }
    key(authenticationState.user?.uid ?: "signed-out") {
        WearWiseNavigationGraph(container = container, authenticatedUser = authenticationState.user)
    }
}

@Composable
private fun WearWiseNavigationGraph(container: AppContainer, authenticatedUser: AuthenticatedUser?) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Outfits.route

    // Observe language, connectivity, and sync state
    val currentLanguage by container.languageManager.currentLanguage.collectAsState()
    val isOnline by container.connectivityMonitor.isOnline.collectAsState()
    val syncState by container.syncManager.syncState.collectAsState()
    val themeMode by container.themePreferences.themeMode.collectAsState()
    val activeProfile by container.userProfileUseCase.getProfile().collectAsState(initial = null)
    val authViewModel = remember { AuthViewModel(container.authentication) }

    val mainDestinations = listOf(
        Screen.Outfits.route,
        Screen.Closet.route,
        Screen.Avatar.route,
        Screen.Planner.route,
        Screen.Shopping.route,
        Screen.Profile.route
    )
    val shouldShowBottomBar = currentRoute in mainDestinations

    // ViewModels with constructor injection via container
    val homeViewModel = remember {
        HomeViewModel(container.getDailyRecommendationUseCase, container.userProfileUseCase, container.manageWardrobeUseCase, container.outfitRepository)
    }
    val wardrobeViewModel = remember {
        WardrobeViewModel(container.manageWardrobeUseCase, container.tryOnFileStore, container.apiKeyManager)
    }
    val tryOnViewModel = remember {
        TryOnViewModel(
            container.executeTryOnUseCase,
            container.manageWardrobeUseCase,
            container.userProfileUseCase,
            container.outfitRepository
        )
    }
    val plannerViewModel = remember {
        PlannerViewModel(container.weeklyPlannerUseCase, container.manageWardrobeUseCase, container.outfitRepository, container.getWeatherUseCase, container.appContext)
    }
    val profileViewModel = remember {
        ProfileViewModel(container.userProfileUseCase, container.apiKeyManager, container.authentication, container.database, container.appContext)
    }
    val shoppingViewModel = remember {
        ShoppingViewModel(
            container.shoppingRepository,
            container.manageWardrobeUseCase,
            container.userProfileUseCase,
            container.getWeatherUseCase
        )
    }
    val colorAnalysisViewModel = remember {
        ColorAnalysisViewModel(
            container.userProfileUseCase,
            container.apiKeyManager,
            container.tryOnFileStore,
            container.appContext
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.data.local.LocalAppLanguage provides currentLanguage,
        com.example.data.local.LocalLanguageManager provides container.languageManager,
        LocalWearWiseProfile provides activeProfile
    ) {
        Scaffold(
            containerColor = DarkNavyBg,
        bottomBar = {
            if (shouldShowBottomBar) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    WearWiseBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { targetRoute ->
                            if (currentRoute != targetRoute) {
                                navController.navigate(targetRoute) {
                                    popUpTo(Screen.Outfits.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Connectivity / sync status indicator — shown on all main screens
            if (currentRoute in mainDestinations) {
                OfflineStatusBar(
                    isOnline = isOnline,
                    syncState = syncState
                )
            }

            NavHost(
                navController = navController,
                startDestination = if (authenticatedUser != null) Screen.Outfits.route else Screen.Welcome.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onNavigateToCreateAccount = {
                            navController.navigate(Screen.CreateAccount.route)
                        },
                        onNavigateToSignIn = {
                            navController.navigate(Screen.SignIn.route)
                        }
                    )
                }

                composable(Screen.CreateAccount.route) {
                    FirebaseAccountScreen(
                        viewModel = authViewModel,
                        signIn = false,
                        onSuccess = { navController.navigate(Screen.Outfits.route) { popUpTo(Screen.Welcome.route) { inclusive = true } } },
                        onToggleMode = { navController.navigate(Screen.SignIn.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SignIn.route) {
                    FirebaseAccountScreen(
                        viewModel = authViewModel,
                        signIn = true,
                        onSuccess = { navController.navigate(Screen.Outfits.route) { popUpTo(Screen.Welcome.route) { inclusive = true } } },
                        onToggleMode = { navController.navigate(Screen.CreateAccount.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onComplete = {
                            navController.navigate(Screen.Outfits.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Outfits.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToCloset = { navController.navigate(Screen.Closet.route) },
                        onNavigateToAvatar = { navController.navigate(Screen.Avatar.route) },
                        onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                    )
                }

                composable(Screen.Closet.route) {
                    WardrobeScreen(
                        viewModel = wardrobeViewModel,
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onUseInTryOn = { item ->
                            tryOnViewModel.addGarment(item)
                            navController.navigate(Screen.Avatar.route)
                        }
                    )
                }

                composable(Screen.Avatar.route) {
                    TryOnScreen(
                        viewModel = tryOnViewModel,
                        isOnline = isOnline,
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                    )
                }

                composable(Screen.Planner.route) {
                    PlannerScreen(
                        viewModel = plannerViewModel,
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onUseInTryOn = { outfit ->
                            tryOnViewModel.applyOutfit(outfit)
                            navController.navigate(Screen.Avatar.route)
                        },
                        onNavigateToTrips = { navController.navigate(Screen.Trips.route) }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        themeMode = themeMode,
                        emailVerified = authenticatedUser?.emailVerified == true,
                        onThemeModeChanged = container.themePreferences::setThemeMode,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToColorAnalysis = { navController.navigate(Screen.ColorAnalysis.route) },
                        onNavigateToTrips = { navController.navigate(Screen.Trips.route) },
                        onSignOut = {
                            container.authentication.signOut()
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Shopping.route) {
                    ShoppingScreen(
                        viewModel = shoppingViewModel,
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                    )
                }
                composable(Screen.ColorAnalysis.route) {
                    ColorAnalysisScreen(
                        viewModel = colorAnalysisViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Trips.route) {
                    TripsScreen(
                        viewModel = remember { TripsViewModel(container.tripRepository) },
                        onNavigateToCreateTrip = { navController.navigate(Screen.CreateTrip.route) },
                        onNavigateToTripDetail = { tripId -> navController.navigate(Screen.TripDetail.createRoute(tripId)) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.CreateTrip.route) {
                    CreateTripScreen(
                        viewModel = remember {
                            CreateTripViewModel(
                                container.tripRepository,
                                container.wardrobeRepository,
                                container.userRepository,
                                container.weatherRepository
                            )
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onTripCreated = { tripId ->
                            navController.navigate(Screen.TripDetail.createRoute(tripId)) {
                                popUpTo(Screen.Trips.route)
                            }
                        }
                    )
                }
                composable(
                    route = Screen.TripDetail.route,
                    arguments = listOf(navArgument("tripId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                    TripDetailScreen(
                        viewModel = remember(tripId) {
                            TripDetailViewModel(container.tripRepository, tripId)
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                }
            }
        }
    }
}

