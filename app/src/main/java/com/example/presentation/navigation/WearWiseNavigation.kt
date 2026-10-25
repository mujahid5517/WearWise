package com.example.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.presentation.authentication.CreateAccountScreen
import com.example.presentation.authentication.WelcomeScreen
import com.example.presentation.common.WearWiseBottomBar
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.planner.PlannerScreen
import com.example.presentation.planner.PlannerViewModel
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.profile.ProfileViewModel
import com.example.presentation.tryon.TryOnScreen
import com.example.presentation.tryon.TryOnViewModel
import com.example.presentation.wardrobe.WardrobeScreen
import com.example.presentation.wardrobe.WardrobeViewModel
import com.example.ui.theme.DarkNavyBg

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object CreateAccount : Screen("create_account")
    data object Onboarding : Screen("onboarding")
    data object Outfits : Screen("outfits")
    data object Closet : Screen("closet")
    data object Avatar : Screen("avatar")
    data object Planner : Screen("planner")
    data object Profile : Screen("profile")
}

@Composable
fun WearWiseApp(container: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Outfits.route

    val mainDestinations = listOf(
        Screen.Outfits.route,
        Screen.Closet.route,
        Screen.Avatar.route,
        Screen.Planner.route,
        Screen.Profile.route
    )
    val shouldShowBottomBar = currentRoute in mainDestinations

    // ViewModels with constructor injection via container
    val homeViewModel = remember {
        HomeViewModel(container.getDailyRecommendationUseCase, container.userProfileUseCase, container.manageWardrobeUseCase)
    }
    val wardrobeViewModel = remember {
        WardrobeViewModel(container.manageWardrobeUseCase)
    }
    val tryOnViewModel = remember {
        TryOnViewModel(container.executeTryOnUseCase, container.manageWardrobeUseCase, container.userProfileUseCase)
    }
    val plannerViewModel = remember {
        PlannerViewModel(container.weeklyPlannerUseCase)
    }
    val profileViewModel = remember {
        ProfileViewModel(container.userProfileUseCase, container.apiKeyManager)
    }

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
        NavHost(
            navController = navController,
            startDestination = Screen.Outfits.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNavigateToCreateAccount = {
                        navController.navigate(Screen.CreateAccount.route)
                    },
                    onNavigateToSignIn = {
                        navController.navigate(Screen.Outfits.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.CreateAccount.route) {
                CreateAccountScreen(
                    onContinue = {
                        navController.navigate(Screen.Onboarding.route)
                    },
                    onBack = { navController.popBackStack() },
                    onSignIn = {
                        navController.navigate(Screen.Outfits.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
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
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.Avatar.route) {
                TryOnScreen(
                    viewModel = tryOnViewModel,
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.Planner.route) {
                PlannerScreen(
                    viewModel = plannerViewModel,
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSignOut = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
