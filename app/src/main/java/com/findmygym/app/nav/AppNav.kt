package com.findmygym.app.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.ui.components.AppDrawerScaffold
import com.findmygym.app.ui.leaderboard.LeaderboardScreen
import com.findmygym.app.ui.login.LoginScreen
import com.findmygym.app.ui.login.RegisterScreen
import com.findmygym.app.ui.map.MapScreen
import com.findmygym.app.ui.profile.ProfileScreen
import com.findmygym.app.ui.splash.SplashScreen

@Composable
fun AppNav() {
    val navController = rememberNavController()

    var requestGymList by rememberSaveable { mutableStateOf(false) }
    var requestFilters by rememberSaveable { mutableStateOf(false) }
    var requestAddGym by rememberSaveable { mutableStateOf(false) }

    var focusLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var focusLng by rememberSaveable { mutableStateOf<Double?>(null) }

    fun goToMap() {
        navController.navigate(Routes.MAP) { launchSingleTop = true }
    }

    fun focusGymOnMap(lat: Double, lng: Double) {
        focusLat = lat
        focusLng = lng
        goToMap()
    }

    fun goToSplashClearBackstack() {
        navController.navigate(Routes.SPLASH) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onGoLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onGoMap = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onGoRegister = { navController.navigate(Routes.REGISTER) },
                onGoMap = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackToLogin = { navController.popBackStack() },
                onGoMap = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAP) {
            val authRepo = AuthRepository()

            AppDrawerScaffold(
                title = "Map",
                currentRoute = Routes.MAP,

                onGoMap = { navController.navigate(Routes.MAP) { launchSingleTop = true } },
                onGoProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { navController.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                onGoGymList = { requestGymList = true },
                onGoAddGym = { requestAddGym = true },

                onOpenFilters = { requestFilters = true },

                onLogout = {
                    authRepo.logout()
                    goToSplashClearBackstack()
                }
            ) { inner ->
                MapScreen(
                    modifier = Modifier.padding(inner),
                    requestGymList = requestGymList,
                    onRequestGymListConsumed = { requestGymList = false },
                    requestFilters = requestFilters,
                    onRequestFiltersConsumed = { requestFilters = false },
                    requestAddGym = requestAddGym,
                    onRequestAddGymConsumed = { requestAddGym = false },

                    focusLat = focusLat,
                    focusLng = focusLng,
                    onFocusConsumed = {
                        focusLat = null
                        focusLng = null
                    }
                )
            }
        }

        composable(Routes.LEADERBOARD) {
            val authRepo = AuthRepository()

            AppDrawerScaffold(
                title = "Leaderboard",
                currentRoute = Routes.LEADERBOARD,

                onGoMap = { goToMap() },
                onGoProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { navController.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                onGoGymList = {
                    requestGymList = true
                    goToMap()
                },
                onGoAddGym = {
                    requestAddGym = true
                    goToMap()
                },

                onOpenFilters = null,

                onLogout = {
                    authRepo.logout()
                    goToSplashClearBackstack()
                }
            ) { inner ->
                LeaderboardScreen(modifier = Modifier.padding(inner))
            }
        }

        composable(Routes.PROFILE) {
            val authRepo = AuthRepository()

            AppDrawerScaffold(
                title = "Profile",
                currentRoute = Routes.PROFILE,

                onGoMap = { goToMap() },
                onGoProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { navController.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                onGoGymList = {
                    requestGymList = true
                    goToMap()
                },
                onGoAddGym = {
                    requestAddGym = true
                    goToMap()
                },

                onOpenFilters = null,

                onLogout = {
                    authRepo.logout()
                    goToSplashClearBackstack()
                }
            ) { inner ->
                ProfileScreen(
                    modifier = Modifier.padding(inner),
                    onFocusGym = { lat, lng -> focusGymOnMap(lat, lng) },
                    onAccountDeleted = { goToSplashClearBackstack() }
                )
            }
        }
    }
}
