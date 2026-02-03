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
    val nav = rememberNavController()

    // Global triggers for MapScreen UI
    var requestGymList by rememberSaveable { mutableStateOf(false) }
    var requestFilters by rememberSaveable { mutableStateOf(false) }
    var requestAddGym by rememberSaveable { mutableStateOf(false) }

    fun goToMap() {
        nav.navigate(Routes.MAP) { launchSingleTop = true }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onGoLogin = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onGoMap = {
                    nav.navigate(Routes.MAP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onGoRegister = { nav.navigate(Routes.REGISTER) },
                onGoMap = {
                    nav.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackToLogin = { nav.popBackStack() },
                onGoMap = {
                    nav.navigate(Routes.MAP) {
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

                // left menu navigation
                onGoProfile = { nav.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { nav.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                // actions that should happen on map
                onGoGymList = { requestGymList = true },
                onGoAddGym = { requestAddGym = true },

                // top-right icon only on map
                onOpenFilters = { requestFilters = true },

                onLogout = {
                    authRepo.logout()
                    nav.navigate(Routes.SPLASH) {
                        popUpTo(nav.graph.id) { inclusive = true }
                    }
                }
            ) { inner ->
                MapScreen(
                    modifier = Modifier.padding(inner),
                    requestGymList = requestGymList,
                    onRequestGymListConsumed = { requestGymList = false },
                    requestFilters = requestFilters,
                    onRequestFiltersConsumed = { requestFilters = false },
                    requestAddGym = requestAddGym,
                    onRequestAddGymConsumed = { requestAddGym = false }
                )
            }
        }

        composable(Routes.LEADERBOARD) {
            val authRepo = AuthRepository()

            AppDrawerScaffold(
                title = "Leaderboard",
                currentRoute = Routes.LEADERBOARD,

                onGoProfile = { nav.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { nav.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                // from leaderboard: go to map + trigger
                onGoGymList = {
                    requestGymList = true
                    goToMap()
                },
                onGoAddGym = {
                    requestAddGym = true
                    goToMap()
                },

                // no filter icon here
                onOpenFilters = null,

                onLogout = {
                    authRepo.logout()
                    nav.navigate(Routes.SPLASH) {
                        popUpTo(nav.graph.id) { inclusive = true }
                    }
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

                onGoProfile = { nav.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { nav.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                // from profile: go to map + trigger
                onGoGymList = {
                    requestGymList = true
                    goToMap()
                },
                onGoAddGym = {
                    requestAddGym = true
                    goToMap()
                },

                // no filter icon here
                onOpenFilters = null,

                onLogout = {
                    authRepo.logout()
                    nav.navigate(Routes.SPLASH) {
                        popUpTo(nav.graph.id) { inclusive = true }
                    }
                }
            ) { inner ->
                ProfileScreen(modifier = Modifier.padding(inner))
            }
        }
    }
}
