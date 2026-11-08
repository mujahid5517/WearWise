package com.example.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.ViewModelStore
import com.example.di.AppContainer
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.ProfileSyncStatus
import com.example.presentation.authentication.FirebaseAccountScreen
import com.example.presentation.authentication.AuthViewModel
import com.example.presentation.authentication.WelcomeScreen
import com.example.presentation.common.OfflineStatusBar
import com.example.presentation.common.LocalWearWiseProfile
import com.example.presentation.common.WearWiseBottomBar
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.feedback.FeedbackScreen
import com.example.presentation.feedback.FeedbackViewModel
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.planner.PlannerScreen
import com.example.presentation.planner.PlannerViewModel
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.profile.ProfileViewModel
import com.example.presentation.shopping.AssistantFloatingButton
import com.example.presentation.shopping.EmptyShopScreen
import com.example.presentation.shopping.ShoppingWishlistScreen
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
    data object AssistantChat : Screen("assistant_chat")
    data object ColorAnalysis : Screen("color_analysis")
    data object Trips : Screen("trips")
    data object CreateTrip : Screen("create_trip")
    data object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: Long) = "trip_detail/$tripId"
    }
    data object Feedback : Screen("feedback")
}

internal fun shouldShowAssistantButton(currentRoute: String): Boolean =
    currentRoute !in setOf(
        Screen.Welcome.route,
        Screen.CreateAccount.route,
        Screen.SignIn.route,
        Screen.Onboarding.route,
        Screen.AssistantChat.route
    )

@Composable
fun WearWiseApp(container: AppContainer) {
    val authenticationState by container.authentication.state.collectAsState()
    if (!authenticationState.isReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
        }
        return
    }
    key("${authenticationState.user?.uid ?: "signed-out"}:${authenticationState.profileSyncStatus}") {
        WearWiseNavigationGraph(
            container = container,
            authenticatedUser = authenticationState.user,
            profileSyncStatus = authenticationState.profileSyncStatus
        )
    }
}

@Composable
private fun WearWiseNavigationGraph(
    container: AppContainer,
    authenticatedUser: AuthenticatedUser?,
    profileSyncStatus: ProfileSyncStatus
) {
    val navController = rememberNavController()
    val viewModelStore = remember { ViewModelStore() }
    DisposableEffect(viewModelStore) {
        onDispose { viewModelStore.clear() }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Outfits.route

    // Observe language, connectivity, and sync state
    val currentLanguage by container.languageManager.currentLanguage.collectAsState()
    val isOnline by container.connectivityMonitor.isOnline.collectAsState()
    val syncState by container.syncManager.syncState.collectAsState()
    val themeMode by container.themePreferences.themeMode.collectAsState()
    val activeProfile by container.userProfileUseCase.getProfile().collectAsState(initial = null)
    val authViewModel = remember(viewModelStore) {
        AuthViewModel(container.authentication).also { viewModelStore.put("auth", it) }
    }

    val mainDestinations = listOf(
        Screen.Outfits.route,
        Screen.Closet.route,
        Screen.Avatar.route,
        Screen.Planner.route,
        Screen.Shopping.route,
        Screen.Profile.route
    )
    val shouldShowBottomBar = currentRoute in mainDestinations
    val showAssistantButton = shouldShowAssistantButton(currentRoute)

    // ViewModels with constructor injection via container
    val homeViewModel = remember(viewModelStore) {
        HomeViewModel(
            container.getDailyRecommendationUseCase,
            container.userProfileUseCase,
            container.manageWardrobeUseCase,
            container.outfitRepository,
            container.outfitFeedbackRepository,
            container.executeTryOnUseCase,
            container.connectivityMonitor.isOnline
        ).also { viewModelStore.put("home", it) }
    }
    val wardrobeViewModel = remember(viewModelStore) {
        WardrobeViewModel(container.manageWardrobeUseCase, container.tryOnFileStore, container.apiKeyManager)
            .also { viewModelStore.put("wardrobe", it) }
    }
    val tryOnViewModel = remember(viewModelStore) {
        TryOnViewModel(
            container.executeTryOnUseCase,
            container.manageWardrobeUseCase,
            container.userProfileUseCase,
            container.outfitRepository
        ).also { viewModelStore.put("try-on", it) }
    }
    val plannerViewModel = remember(viewModelStore) {
        PlannerViewModel(container.weeklyPlannerUseCase, container.manageWardrobeUseCase, container.outfitRepository, container.getWeatherUseCase, container.appContext)
            .also { viewModelStore.put("planner", it) }
    }
    val profileViewModel = remember(viewModelStore) {
        ProfileViewModel(container.userProfileUseCase, container.apiKeyManager, container.authentication, container.database, container.appContext)
            .also { viewModelStore.put("profile", it) }
    }
    val shoppingViewModel = remember(viewModelStore) {
        ShoppingViewModel(
            container.shoppingRepository,
            container.manageWardrobeUseCase,
            container.userProfileUseCase,
            container.getWeatherUseCase
        ).also { viewModelStore.put("shopping", it) }
    }
    val colorAnalysisViewModel = remember(viewModelStore) {
        ColorAnalysisViewModel(
            container.userProfileUseCase,
            container.apiKeyManager,
            container.tryOnFileStore,
            container.appContext
        ).also { viewModelStore.put("color-analysis", it) }
    }
    val feedbackViewModel = remember(viewModelStore) {
        FeedbackViewModel(container.outfitFeedbackRepository).also { viewModelStore.put("feedback", it) }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Connectivity / sync status indicator — shown on all main screens
                if (currentRoute in mainDestinations) {
                    OfflineStatusBar(
                        isOnline = isOnline,
                        syncState = syncState
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = if (
                        authenticatedUser != null && profileSyncStatus == ProfileSyncStatus.SYNCED
                    ) {
                        Screen.Outfits.route
                    } else {
                        Screen.SignIn.route
                    },
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
                        onSuccess = { navController.navigate(Screen.Outfits.route) { popUpTo(0) { inclusive = true } } },
                        onToggleMode = { navController.navigate(Screen.SignIn.route) },
                        onBack = { navController.navigate(Screen.Welcome.route) }
                    )
                }

                composable(Screen.SignIn.route) {
                    FirebaseAccountScreen(
                        viewModel = authViewModel,
                        signIn = true,
                        onSuccess = { navController.navigate(Screen.Outfits.route) { popUpTo(0) { inclusive = true } } },
                        onToggleMode = { navController.navigate(Screen.CreateAccount.route) },
                        onBack = { navController.navigate(Screen.Welcome.route) }
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
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onNavigateToFeedback = { navController.navigate(Screen.Feedback.route) }
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
                            navController.navigate(Screen.SignIn.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Shopping.route) {
                    ShoppingWishlistScreen(
                        viewModel = shoppingViewModel,
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                    )
                }
                composable(Screen.AssistantChat.route) {
                    ShoppingScreen(
                        viewModel = shoppingViewModel,
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        showCloseButton = true,
                        onNavigateBack = { navController.popBackStack() }
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
                        viewModel = remember(viewModelStore) {
                            TripsViewModel(container.tripRepository).also { viewModelStore.put("trips", it) }
                        },
                        onNavigateToCreateTrip = { navController.navigate(Screen.CreateTrip.route) },
                        onNavigateToTripDetail = { tripId -> navController.navigate(Screen.TripDetail.createRoute(tripId)) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.CreateTrip.route) {
                    CreateTripScreen(
                        viewModel = remember(viewModelStore) {
                            CreateTripViewModel(
                                container.tripRepository,
                                container.wardrobeRepository,
                                container.userRepository,
                                container.weatherRepository
                            ).also { viewModelStore.put("create-trip", it) }
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
                        viewModel = remember(viewModelStore, tripId) {
                            TripDetailViewModel(container.tripRepository, tripId)
                                .also { viewModelStore.put("trip-detail-$tripId", it) }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Feedback.route) {
                    FeedbackScreen(viewModel = feedbackViewModel, onNavigateToProfile = { navController.navigate(Screen.Profile.route) })
                }
                }
            }
            if (showAssistantButton) {
                AssistantFloatingButton(
                    preferences = container.assistantButtonPreferences,
                    onClick = {
                        navController.navigate(Screen.AssistantChat.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.TopStart)
                )
            }
        }
    }
}
}
